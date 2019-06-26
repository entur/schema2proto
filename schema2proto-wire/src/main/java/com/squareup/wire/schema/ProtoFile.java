/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema;

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

import static com.squareup.wire.schema.Options.FILE_OPTIONS;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.squareup.wire.schema.internal.parser.OptionElement;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;

public final class ProtoFile {
	static final ProtoMember JAVA_PACKAGE = ProtoMember.get(FILE_OPTIONS, "java_package");

	private Location location;
	private List<String> imports = new ArrayList<>();
	private List<String> publicImports = new ArrayList<>();
	private String packageName;
	private List<Type> types = new ArrayList<>();
	private List<Service> services = new ArrayList<>();
	private List<Extend> extendList = new ArrayList<>();
	private Options options;
	private Syntax syntax;
	private Object javaPackage;

	public ProtoFile(Location location, List<String> imports, List<String> publicImports, String packageName, List<Type> types, List<Service> services,
			List<Extend> extendList, Options options, Syntax syntax) {
		this.location = location;
		this.imports.addAll(imports);
		this.publicImports.addAll(publicImports);
		this.packageName = packageName;
		this.types.addAll(types);
		this.services.addAll(services);
		this.extendList.addAll(extendList);
		this.options = options;
		this.syntax = syntax;
	}

	public ProtoFile(Syntax syntax, String packageName) {
		this.syntax = syntax;
		this.packageName = packageName;
		this.options = new Options(ProtoType.BOOL, new ArrayList<OptionElement>());
		this.location = new Location("path", "base", 1, 1);

	}

	static ProtoFile get(ProtoFileElement protoFileElement) {
		String packageName = protoFileElement.getPackageName();

		ImmutableList<Type> types = Type.fromElements(packageName, protoFileElement.getTypes());

		ImmutableList<Service> services = Service.fromElements(packageName, protoFileElement.getServices());

		ImmutableList<Extend> wireExtends = Extend.fromElements(packageName, protoFileElement.getExtendDeclarations());

		Options options = new Options(Options.FILE_OPTIONS, protoFileElement.getOptions());

		return new ProtoFile(protoFileElement.getLocation(), protoFileElement.getImports(), protoFileElement.getPublicImports(), packageName, types, services,
				wireExtends, options, protoFileElement.getSyntax());
	}

	ProtoFileElement toElement() {
		return new ProtoFileElement(location, packageName, syntax, imports, publicImports, Type.toElements(types), Service.toElements(services),
				Extend.toElements(extendList), options.toElements());
	}

	public Location location() {
		return location;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public List<String> imports() {
		return imports;
	}

	public List<String> publicImports() {
		return publicImports;
	}

	/**
	 * Returns the name of this proto file, like {@code simple_message} for {@code
	 * squareup/protos/person/simple_message.proto}.
	 */
	public String name() {
		String result = location().getPath();

		int slashIndex = result.lastIndexOf('/');
		if (slashIndex != -1) {
			result = result.substring(slashIndex + 1);
		}

		if (result.endsWith(".proto")) {
			result = result.substring(0, result.length() - ".proto".length());
		}

		return result;
	}

	public String packageName() {
		return packageName;
	}

	public String javaPackage() {
		return javaPackage != null ? String.valueOf(javaPackage) : null;
	}

	public List<Type> types() {
		return types;
	}

	public List<Service> services() {
		return services;
	}

	List<Extend> extendList() {
		return extendList;
	}

	public Options options() {
		return options;
	}

	/** Returns a new proto file that omits types and services not in {@code identifiers}. */
	ProtoFile retainAll(Schema schema, MarkSet markSet) {
		ImmutableList.Builder<Type> retainedTypes = ImmutableList.builder();
		for (Type type : types) {
			Type retainedType = type.retainAll(schema, markSet);
			if (retainedType != null) {
				retainedTypes.add(retainedType);
			}
		}

		ImmutableList.Builder<Service> retainedServices = ImmutableList.builder();
		for (Service service : services) {
			Service retainedService = service.retainAll(schema, markSet);
			if (retainedService != null) {
				retainedServices.add(retainedService);
			}
		}

		ProtoFile result = new ProtoFile(location, imports, publicImports, packageName, retainedTypes.build(), retainedServices.build(), extendList,
				options.retainAll(schema, markSet), syntax);
		result.javaPackage = javaPackage;
		return result;
	}

	void linkOptions(Linker linker) {
		options.link(linker);
		javaPackage = options().get(JAVA_PACKAGE);
	}

	@Override
	public String toString() {
		return location().getPath();
	}

	public String toSchema() {
		return toElement().toSchema();
	}

	void validate(Linker linker) {
		linker.validateEnumConstantNameUniqueness(types);
	}

	public Syntax getSyntax() {
		return syntax;
	}

	public List<Extend> getExtendList() {
		return extendList;
	}

	/** Syntax version. */
	public enum Syntax {
		PROTO_2("proto2"),
		PROTO_3("proto3");

		private final String string;

		Syntax(String string) {
			this.string = string;
		}

		public static Syntax get(String string) {
			for (Syntax syntax : values()) {
				if (syntax.string.equals(string))
					return syntax;
			}
			throw new IllegalArgumentException("unexpected syntax: " + string);
		}

		@Override
		public String toString() {
			return string;
		}
	}
}
