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
package no.entur.schema2proto.generateproto.wire;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.SchemaLoader;

/**
 * Adapter that reproduces the vendored {@code SchemaLoader} contract (a set of source roots plus an optional set of specific protos to load) on top of stock
 * wire's {@link SchemaLoader} ({@code initRoots} / {@code loadSchema}).
 *
 * <p>
 * When {@code protos} is empty all protos found under the source roots are loaded; otherwise only the named protos (and their imports) are loaded. Google's
 * well-known types (including {@code descriptor.proto}) are provided by stock wire automatically.
 */
public final class WireSchemaLoader {

	private WireSchemaLoader() {
	}

	public static Schema load(List<Path> sources, List<String> protos) throws IOException {
		SchemaLoader loader = new SchemaLoader(FileSystems.getDefault());

		List<Location> sourcePath = new ArrayList<>();
		List<Location> protoPath = new ArrayList<>();

		if (protos.isEmpty()) {
			for (Path s : sources) {
				sourcePath.add(Location.get(s.toString()));
			}
		} else {
			for (String proto : protos) {
				Location resolved = null;
				for (Path s : sources) {
					if (Files.exists(s.resolve(proto))) {
						resolved = Location.get(s.toString(), proto);
						break;
					}
				}
				sourcePath.add(resolved != null ? resolved : Location.get(proto));
			}
			for (Path s : sources) {
				protoPath.add(Location.get(s.toString()));
			}
		}

		loader.initRoots(sourcePath, protoPath);
		return loader.loadSchema();
	}

	/** Stock wire's {@link Schema} has no {@code protoFileForPackage}; emulate the vendored lookup by package name. */
	public static ProtoFile protoFileForPackage(Schema schema, String packageName) {
		for (ProtoFile file : schema.getProtoFiles()) {
			if (packageName == null ? file.getPackageName() == null : packageName.equals(file.getPackageName())) {
				return file;
			}
		}
		return null;
	}
}
