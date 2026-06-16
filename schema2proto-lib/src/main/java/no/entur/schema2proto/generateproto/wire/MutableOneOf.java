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
import java.util.List;
import java.util.stream.Collectors;

import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.OneOf;
import com.squareup.wire.schema.Options;

/** Mutable builder analogue of {@link com.squareup.wire.schema.OneOf}. */
public class MutableOneOf {

	private final String name;
	private String documentation;
	private final List<MutableField> fields;
	private final MutableOptions options;

	public MutableOneOf(String name, String documentation, List<MutableField> fields, MutableOptions options) {
		this.name = name;
		this.documentation = documentation;
		this.fields = fields != null ? fields : new ArrayList<>();
		this.options = options != null ? options : new MutableOptions(MutableOptions.ONEOF_OPTIONS, new ArrayList<>());
	}

	public String name() {
		return name;
	}

	public String documentation() {
		return documentation;
	}

	public void updateDocumentation(String documentation) {
		this.documentation = documentation;
	}

	public List<MutableField> fields() {
		return fields;
	}

	public void addField(MutableField newField) {
		newField.setOneOf(true);
		fields.add(newField);
	}

	public OneOf toWire() {
		List<com.squareup.wire.schema.Field> wireFields = fields.stream().map(f -> {
			f.setOneOf(true);
			return f.toWire();
		}).collect(Collectors.toList());
		return new OneOf(name, documentation == null ? "" : documentation, wireFields, Location.get("", ""), options.toWire());
	}

	Options optionsToWire() {
		return options.toWire();
	}
}
