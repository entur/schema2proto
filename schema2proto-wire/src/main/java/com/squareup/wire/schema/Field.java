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
package com.squareup.wire.schema;

import static com.squareup.wire.schema.Options.FIELD_OPTIONS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.squareup.wire.schema.internal.parser.FieldElement;

public final class Field {
	static final ProtoMember DEPRECATED = ProtoMember.get(FIELD_OPTIONS, "deprecated");
	static final ProtoMember PACKED = ProtoMember.get(FIELD_OPTIONS, "packed");

	private String packageName;
	private final Location location;
	private Label label;
	private String name;
	private String documentation;
	private int tag;
	private final String defaultValue;
	private String elementType;
	private final boolean extension;
	private final Options options;
	private ProtoType type;
	private Boolean deprecated;
	private Boolean packed;
	private boolean redacted;

	public boolean isFromAttribute() {
		return fromAttribute;
	}

	public void setFromAttribute(boolean fromAttribute) {
		this.fromAttribute = fromAttribute;
	}

	private boolean fromAttribute = false;

	public boolean isFromElement() {
		return fromElement;
	}

	public void setFromElement(boolean fromElement) {
		this.fromElement = fromElement;
	}

	private boolean fromElement = true;

	public Field(String packageName, Location location, Label label, String name, String documentation, int tag, String defaultValue, String elementType,
			Options options, boolean extension, boolean fromElement) {
		this.packageName = packageName;
		this.location = location;
		this.label = label;
		this.name = name;
		this.documentation = documentation;
		this.tag = tag;
		this.defaultValue = defaultValue;
		this.elementType = elementType;
		this.extension = extension;
		this.options = options;
		this.fromElement = fromElement;
	}

	public Field(String packageName, Location location, Label label, String name, String documentation, int tag, String elementType, Options options,
			boolean fromElement) {
		this.packageName = packageName;
		this.location = location;
		this.label = label;
		this.name = name;
		this.documentation = documentation;
		this.tag = tag;
		this.defaultValue = null;
		this.elementType = elementType;
		this.extension = false;
		this.options = options;
		this.fromElement = fromElement;
	}

	static List<Field> fromElements(String packageName, List<FieldElement> fieldElements, boolean extension) {
		List<Field> fields = new ArrayList<>();
		for (FieldElement field : fieldElements) {
			fields.add(new Field(packageName, field.getLocation(), field.getLabel(), field.getName(), field.getDocumentation(), field.getTag(),
					field.getDefaultValue(), field.getType(), new Options(Options.FIELD_OPTIONS, field.getOptions()), extension, false));

		}
		return fields;
	}

	static ImmutableList<FieldElement> toElements(List<Field> fields) {
		ImmutableList.Builder<FieldElement> elements = new ImmutableList.Builder<>();
		for (Field field : fields) {
			elements.add(new FieldElement(field.location, field.label, field.elementType, field.name, field.defaultValue, field.tag, field.documentation,
					field.options.toElements()));
		}
		return elements.build();
	}

	public Location location() {
		return location;
	}

	public String packageName() {
		return packageName;
	}

	public void clearPackageName() {
		this.packageName = null;
	}

	public Label label() {
		return label;
	}

	public boolean isRepeated() {
		return label() == Label.REPEATED;
	}

	public boolean isOptional() {
		return label() == Label.OPTIONAL;
	}

	public boolean isRequired() {
		return label() == Label.REQUIRED;
	}

	public ProtoType type() {
		return type;
	}

	public String name() {
		return name;
	}

	/**
	 * Returns this field's name, prefixed with its package name. Uniquely identifies extension fields, such as in options.
	 */
	public String qualifiedName() {
		return packageName != null ? packageName + '.' + name : name;
	}

	public int tag() {
		return tag;
	}

	public String documentation() {
		return documentation;
	}

	public Options options() {
		return options;
	}

	public boolean isDeprecated() {
		return deprecated != null && deprecated;
	}

	public boolean isPacked() {
		return packed != null && packed;
	}

	public boolean isRedacted() {
		return redacted;
	}

	public String getDefault() {
		return defaultValue;
	}

	private boolean isPackable(Linker linker, ProtoType type) {
		return !type.equals(ProtoType.STRING) && !type.equals(ProtoType.BYTES) && !(linker.get(type) instanceof MessageType);
	}

	public boolean isExtension() {
		return extension;
	}

	void link(Linker linker) {
		linker = linker.withContext(this);
		type = linker.resolveType(elementType);
	}

	void linkOptions(Linker linker) {
		linker = linker.withContext(this);
		options.link(linker);
		deprecated = (Boolean) options().get(DEPRECATED);
		packed = (Boolean) options().get(PACKED);
		// We allow any package name to be used as long as it ends with '.redacted'.
		redacted = options().optionMatches(".*\\.redacted", "true");
	}

	void validate(Linker linker) {
		linker = linker.withContext(this);
		if (isPacked() && !isPackable(linker, type)) {
			linker.addError("packed=true not permitted on %s", type);
		}
		if (extension && isRequired()) {
			linker.addError("extension fields cannot be required", type);
		}
		linker.validateImport(location(), type);
	}

	Field retainAll(Schema schema, MarkSet markSet) {
		// For map types only the value can participate in pruning as the key will always be scalar.
		if (type.isMap() && !markSet.contains(type.valueType())) {
			return null;
		}

		if (!markSet.contains(type)) {
			return null;
		}

		Field result = new Field(packageName, location, label, name, documentation, tag, defaultValue, elementType, options.retainAll(schema, markSet),
				extension, fromElement);
		result.type = type;
		result.deprecated = deprecated;
		result.packed = packed;
		result.redacted = redacted;
		return result;
	}

	static ImmutableList<Field> retainAll(Schema schema, MarkSet markSet, ProtoType enclosingType, Collection<Field> fields) {
		ImmutableList.Builder<Field> result = ImmutableList.builder();
		for (Field field : fields) {
			Field retainedField = field.retainAll(schema, markSet);
			if (retainedField != null && markSet.contains(ProtoMember.get(enclosingType, field.name()))) {
				result.add(retainedField);
			}
		}
		return result.build();
	}

	@Override
	public String toString() {
		return "Field [packageName=" + packageName + ", name=" + name + ", tag=" + tag + ", elementType=" + elementType + "]";
	}

	public void setLabel(Label label) {
		this.label = label;
	}

	public void updateTag(int updatedTag) {
		tag = updatedTag;
	}

	public enum Label {
		OPTIONAL,
		REQUIRED,
		REPEATED,
		/** Indicates the field is a member of a {@code oneof} block. */
		ONE_OF
	}

	public void updateElementType(String newFieldType) {
		elementType = newFieldType;
	}

	public void updatePackageName(String newPackageName) {
		packageName = newPackageName;
	}

	public void updateName(String newFieldName) {
		name = newFieldName;
	}

	public void updateDocumentation(String newDocumentation) {
		documentation = newDocumentation;
	}

	public String getElementType() {
		return elementType;
	}
}
