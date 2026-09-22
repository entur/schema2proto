/*-
 * #%L
 * schema2proto-lib
 * %%
 * Copyright (C) 2019 Entur
 * %%
 * Licensed under the EUPL, Version 1.1 or – as soon they will be
 * approved by the European Commission - subsequent versions of the
 * EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 * http://ec.europa.eu/idabc/eupl5
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 * #L%
 */
package no.entur.schema2proto.wire;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.ProviderNotFoundException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.SchemaLoader;

/**
 * Adapter that reproduces the vendored {@code SchemaLoader} contract (a set of source roots plus an optional set of specific protos to load) on top of stock
 * wire's {@link SchemaLoader} ({@code initRoots} / {@code loadSchema}).
 *
 * <p>
 * When {@code protos} is empty all protos found under the source roots are loaded; otherwise only the named protos (and their imports) are loaded, whatever
 * their extension. Each file is resolved against the first source root that contains it and is loaded only once, even when the same relative path exists in
 * several roots (the vendored loader deduplicated the same way). This matters because extension definitions (e.g. {@code xsd/xsd.proto}) are frequently present
 * in more than one root, and stock wire rejects a duplicated extension field. A source root may be a directory or an archive (.zip/.jar), as in the vendored
 * loader. Google's well-known types (including {@code descriptor.proto}) are provided by stock wire automatically.
 */
public final class WireSchemaLoader {

	private WireSchemaLoader() {
	}

	public static Schema load(List<Path> sources, List<String> protos) throws IOException {
		SchemaLoader loader = new SchemaLoader(FileSystems.getDefault());

		// Map each relative file path to the first source root that contains it (first root wins; deduplicates across roots).
		Map<String, Path> fileToRoot = new LinkedHashMap<>();
		for (Path root : sources) {
			if (Files.isDirectory(root)) {
				try (Stream<Path> walk = Files.walk(root)) {
					indexFiles(walk, root, root, fileToRoot);
				}
			} else if (Files.isRegularFile(root)) {
				// An archive root (.zip/.jar), as supported by the vendored loader. Stock wire resolves a Location whose base is an archive by opening
				// the archive itself, so the entries are indexed here with the archive as their root.
				try (FileSystem archive = FileSystems.newFileSystem(root, (ClassLoader) null)) {
					for (Path archiveRoot : archive.getRootDirectories()) {
						try (Stream<Path> walk = Files.walk(archiveRoot)) {
							indexFiles(walk, archiveRoot, root, fileToRoot);
						}
					}
				} catch (IOException | ProviderNotFoundException e) {
					throw new IllegalArgumentException("Source root is neither a directory nor a readable archive: " + root, e);
				}
			} else {
				throw new IllegalArgumentException("Source root does not exist: " + root);
			}
		}

		// With no protos named, every proto under the source roots is loaded. Files with other extensions stay in the index so that an explicitly requested
		// file, or a transitive import such as {@code import "schema.protodevel";}, still resolves by its exact path, as it did in the vendored loader.
		Set<String> loadSet = new LinkedHashSet<>();
		if (protos.isEmpty()) {
			for (String path : fileToRoot.keySet()) {
				if (path.endsWith(".proto")) {
					loadSet.add(path);
				}
			}
		} else {
			loadSet.addAll(protos);
		}

		List<Location> sourcePath = new ArrayList<>();
		for (String proto : loadSet) {
			Path root = fileToRoot.get(proto);
			if (root == null) {
				throw new java.io.FileNotFoundException("Failed to locate " + proto + " in " + sources);
			}
			sourcePath.add(Location.get(root.toString(), proto));
		}

		// Remaining files are available for transitive imports, added as individual (deduplicated) entries so the same path is never offered twice. Stock wire
		// resolves a proto path entry lazily, by exact path, so carrying non-proto files here costs nothing until something imports one.
		List<Location> protoPath = new ArrayList<>();
		for (Map.Entry<String, Path> entry : fileToRoot.entrySet()) {
			if (!loadSet.contains(entry.getKey())) {
				protoPath.add(Location.get(entry.getValue().toString(), entry.getKey()));
			}
		}

		loader.initRoots(sourcePath, protoPath);
		return loader.loadSchema();
	}

	/**
	 * Records every regular file below {@code walkRoot} under its {@code walkRoot}-relative, slash-separated path, attributing it to {@code sourceRoot}.
	 * Directories are skipped, so one named e.g. {@code messages.proto} is never offered to wire as a schema source.
	 */
	private static void indexFiles(Stream<Path> walk, Path walkRoot, Path sourceRoot, Map<String, Path> fileToRoot) {
		walk.filter(Files::isRegularFile).forEach(p -> fileToRoot.putIfAbsent(walkRoot.relativize(p).toString().replace('\\', '/'), sourceRoot));
	}

}
