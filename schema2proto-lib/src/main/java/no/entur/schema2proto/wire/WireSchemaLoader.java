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
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
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
 * When {@code protos} is empty all protos found under the source roots are loaded; otherwise only the named protos (and their imports) are loaded. Each proto
 * is resolved against the first source root that contains it and is loaded only once, even when the same relative path exists in several roots (the vendored
 * loader deduplicated the same way). This matters because extension definitions (e.g. {@code xsd/xsd.proto}) are frequently present in more than one root, and
 * stock wire rejects a duplicated extension field. Google's well-known types (including {@code descriptor.proto}) are provided by stock wire automatically.
 */
public final class WireSchemaLoader {

	private WireSchemaLoader() {
	}

	public static Schema load(List<Path> sources, List<String> protos) throws IOException {
		SchemaLoader loader = new SchemaLoader(FileSystems.getDefault());

		// Map each relative proto path to the first source root that contains it (first root wins; deduplicates across roots).
		Map<String, Path> protoToRoot = new LinkedHashMap<>();
		for (Path root : sources) {
			if (!Files.isDirectory(root)) {
				continue;
			}
			try (Stream<Path> walk = Files.walk(root)) {
				walk.filter(p -> p.toString().endsWith(".proto")).forEach(p -> protoToRoot.putIfAbsent(root.relativize(p).toString(), root));
			}
		}

		Set<String> loadSet = protos.isEmpty() ? new LinkedHashSet<>(protoToRoot.keySet()) : new LinkedHashSet<>(protos);

		List<Location> sourcePath = new ArrayList<>();
		for (String proto : loadSet) {
			Path root = protoToRoot.get(proto);
			sourcePath.add(root != null ? Location.get(root.toString(), proto) : Location.get(proto));
		}

		// Remaining protos are available for transitive imports, added as individual (deduplicated) files so the same path is never offered twice.
		List<Location> protoPath = new ArrayList<>();
		for (Map.Entry<String, Path> entry : protoToRoot.entrySet()) {
			if (!loadSet.contains(entry.getKey())) {
				protoPath.add(Location.get(entry.getValue().toString(), entry.getKey()));
			}
		}

		loader.initRoots(sourcePath, protoPath);
		return loader.loadSchema();
	}

}
