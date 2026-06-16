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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.squareup.wire.Syntax;
import com.squareup.wire.schema.Extend;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.Service;
import com.squareup.wire.schema.Type;

/** Mutable builder analogue of {@link com.squareup.wire.schema.ProtoFile} used while generating proto files from XSD. */
public class MutableProtoFile {

	private Location location;
	private final List<String> imports = new ArrayList<>();
	private final List<String> publicImports = new ArrayList<>();
	private final String packageName;
	private final List<MutableType> types = new ArrayList<>();
	// Extends and services are not produced by the XSD-to-proto path; they are carried through unchanged when modifying existing protos.
	private final List<Extend> extendList = new ArrayList<>();
	private final List<Service> services = new ArrayList<>();
	private final MutableOptions options;
	private final Syntax syntax;

	public MutableProtoFile(Syntax syntax, String packageName) {
		this.syntax = syntax;
		this.packageName = packageName;
		this.location = Location.get("", "");
		this.options = new MutableOptions(MutableOptions.FILE_OPTIONS, new ArrayList<>());
	}

	public List<MutableType> types() {
		return types;
	}

	public List<String> imports() {
		return imports;
	}

	public List<String> publicImports() {
		return publicImports;
	}

	public String packageName() {
		return packageName;
	}

	public MutableOptions options() {
		return options;
	}

	public List<Extend> getExtendList() {
		return extendList;
	}

	public List<Service> getServices() {
		return services;
	}

	public Location location() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public void mergeFrom(MutableProtoFile source) {
		if (this.syntax != source.syntax) {
			throw new IllegalArgumentException("Source and destination protos must follow the same syntax level (proto2/proto3)");
		}

		java.util.SortedSet<String> mergedImports = new java.util.TreeSet<>(imports);
		mergedImports.addAll(source.imports);
		mergedImports.remove(location.getPath()); // Remove any imports to one self
		imports.clear();
		imports.addAll(mergedImports);

		java.util.SortedSet<String> mergedPublicImports = new java.util.TreeSet<>(publicImports);
		mergedPublicImports.addAll(source.publicImports);
		publicImports.clear();
		publicImports.addAll(mergedPublicImports);

		types.addAll(source.types);
		extendList.addAll(source.extendList);
		services.addAll(source.services);
	}

	public ProtoFile toWire() {
		List<Type> wireTypes = types.stream().map(t -> t.toWire(syntax)).collect(Collectors.toList());
		return new ProtoFile(location, new ArrayList<>(imports), new ArrayList<>(publicImports), Collections.emptyList() /* weakImports */, packageName,
				wireTypes, new ArrayList<>(services), new ArrayList<>(extendList), options.toWire(), syntax);
	}

	public String toSchema() {
		return toWire().toSchema();
	}

	@Override
	public String toString() {
		return location.getPath();
	}

	public String name() {
		String result = location.getPath();
		int slashIndex = result.lastIndexOf('/');
		if (slashIndex != -1) {
			result = result.substring(slashIndex + 1);
		}
		if (result.endsWith(".proto")) {
			result = result.substring(0, result.length() - ".proto".length());
		}
		return result;
	}
}
