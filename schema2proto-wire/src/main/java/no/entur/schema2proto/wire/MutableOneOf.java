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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.internal.parser.FieldElement;
import com.squareup.wire.schema.internal.parser.OneOfElement;

/** Mutable builder analogue of {@link com.squareup.wire.schema.OneOf}. */
public class MutableOneOf {

	private final String name;
	private String documentation;
	private final List<MutableField> fields;
	private final MutableOptions options;
	/** The element this oneOf was built from, if any. See {@link MutableType#toElement()}. */
	private OneOfElement sourceElement;

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
		fields.add(newField);
	}

	void setSourceElement(OneOfElement sourceElement) {
		this.sourceElement = sourceElement;
	}

	public OneOfElement toElement(AtomicInteger order) {
		List<FieldElement> fieldElements = fields.stream().map(f -> f.toElement(order.getAndIncrement())).collect(Collectors.toList());
		String doc = documentation == null ? "" : documentation;
		if (sourceElement != null) {
			return sourceElement.copy(name, doc, fieldElements, sourceElement.getGroups(), options.toElements(), sourceElement.getLocation());
		}
		return new OneOfElement(name, doc, fieldElements, List.of(), options.toElements(), Location.get("", ""));
	}
}
