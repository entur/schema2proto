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

import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.squareup.wire.schema.internal.parser.GroupElement;
import com.squareup.wire.schema.internal.parser.OneOfElement;

public final class OneOf {
	private final String name;
	private String documentation;
	private final List<Field> fields;

	public OneOf(String name, String documentation, List<Field> fields) {
		this.name = name;
		this.documentation = documentation;
		this.fields = fields;
	}

	public String name() {
		return name;
	}

	public String documentation() {
		return documentation;
	}

	public List<Field> fields() {
		return fields;
	}

	void link(Linker linker) {
		for (Field field : fields) {
			field.link(linker);
		}
	}

	void linkOptions(Linker linker) {
		for (Field field : fields) {
			field.linkOptions(linker);
		}
	}

	OneOf retainAll(Schema schema, MarkSet markSet, ProtoType enclosingType) {
		ImmutableList<Field> retainedFields = Field.retainAll(schema, markSet, enclosingType, fields);
		if (retainedFields.isEmpty())
			return null;
		return new OneOf(name, documentation, retainedFields);
	}

	static ImmutableList<OneOf> fromElements(String packageName, List<OneOfElement> elements, boolean extension) {
		ImmutableList.Builder<OneOf> oneOfs = ImmutableList.builder();
		for (OneOfElement oneOf : elements) {
			if (!oneOf.getGroups().isEmpty()) {
				GroupElement group = oneOf.getGroups().get(0);
				throw new IllegalStateException(group.getLocation() + ": 'group' is not supported");
			}
			oneOfs.add(new OneOf(oneOf.getName(), oneOf.getDocumentation(), Field.fromElements(packageName, oneOf.getFields(), extension)));
		}
		return oneOfs.build();
	}

	static ImmutableList<OneOfElement> toElements(List<OneOf> oneOfs) {
		ImmutableList.Builder<OneOfElement> elements = new ImmutableList.Builder<>();
		for (OneOf oneOf : oneOfs) {
			elements.add(new OneOfElement(oneOf.name, oneOf.documentation, Field.toElements(oneOf.fields), Collections.emptyList() // groups
			));
		}
		return elements.build();
	}

	public void updateDocumentation(String documentation) {
		this.documentation = documentation;
	}

	@Override
	public String toString() {
		return "OneOf [" + "name='" + name + '\'' + ", documentation='" + documentation + '\'' + ", fields=" + fields + ']';
	}

	public void addField(Field newField) {
		fields.add(newField);
	}
}
