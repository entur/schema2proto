/*-
 * #%L
 * schema2proto-wire
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads a {@link MutableSchema}, with the builder shape of the vendored wire fork's {@code SchemaLoader}: add source roots (directories or .zip/.jar archives),
 * optionally name the protos to load, then {@link #load()}. See {@link WireSchemaLoader#load(List, List)} for how roots and protos are resolved.
 */
public final class MutableSchemaLoader {

	private final List<Path> sources = new ArrayList<>();
	private final List<String> protos = new ArrayList<>();

	public MutableSchemaLoader addSource(File source) {
		return addSource(source.toPath());
	}

	public MutableSchemaLoader addSource(Path source) {
		sources.add(source);
		return this;
	}

	/** Loads only this proto (and its imports) instead of every proto under the source roots. May be called several times. */
	public MutableSchemaLoader addProto(String proto) {
		protos.add(proto);
		return this;
	}

	public MutableSchema load() throws IOException {
		return WireSchemaLoader.loadMutable(sources, protos);
	}
}
