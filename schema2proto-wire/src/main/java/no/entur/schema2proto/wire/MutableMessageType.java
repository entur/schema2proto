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
import com.squareup.wire.schema.ProtoType;
import com.squareup.wire.schema.Reserved;
import com.squareup.wire.schema.internal.parser.ExtendElement;
import com.squareup.wire.schema.internal.parser.ExtensionsElement;
import com.squareup.wire.schema.internal.parser.FieldElement;
import com.squareup.wire.schema.internal.parser.MessageElement;
import com.squareup.wire.schema.internal.parser.OneOfElement;
import com.squareup.wire.schema.internal.parser.TypeElement;

/** Mutable builder analogue of {@link com.squareup.wire.schema.MessageType}. */
public class MutableMessageType extends MutableType {

	// schema2proto-specific constants, formerly bolted onto the vendored wire MessageType.
	public static final String XSD_MESSAGE_OPTIONS_PACKAGE = "xsd";
	public static final String BASE_TYPE_MESSAGE_OPTION = "base_type";
	public static final String XSD_BASE_TYPE_MESSAGE_OPTION_NAME = XSD_MESSAGE_OPTIONS_PACKAGE + "." + BASE_TYPE_MESSAGE_OPTION;
	public static final int FIELD_NUM_INCREMENT = 10;

	private ProtoType protoType;
	private final Location location;
	private String documentation;
	private String name;
	private List<MutableField> declaredFields;
	private final List<MutableOneOf> oneOfs;
	private final List<MutableType> nestedTypes;
	// Nested extend declarations and extension ranges are not produced by the XSD-to-proto path; they are carried through unchanged when modifying
	// existing proto2 messages.
	private final List<ExtendElement> nestedExtendList;
	private final List<ExtensionsElement> extensionsList;
	private final List<Reserved> reserveds;
	private final MutableOptions options;
	/** The element this message was built from, if any. See {@link MutableType#toElement()}. */
	private MessageElement sourceElement;

	private int fieldNum = 0;
	private boolean wrapperMessageType = false;

	public MutableMessageType(ProtoType protoType, Location location, String documentation, String name, MutableOptions options) {
		this.protoType = protoType;
		this.location = location;
		this.documentation = documentation;
		this.name = name;
		this.options = options;
		this.declaredFields = new ArrayList<>();
		this.oneOfs = new ArrayList<>();
		this.nestedTypes = new ArrayList<>();
		this.nestedExtendList = new ArrayList<>();
		this.extensionsList = new ArrayList<>();
		this.reserveds = new ArrayList<>();
	}

	public boolean isWrapperMessageType() {
		return wrapperMessageType;
	}

	public void setWrapperMessageType(boolean wrapperMessageType) {
		this.wrapperMessageType = wrapperMessageType;
	}

	public List<Reserved> getReserveds() {
		return reserveds;
	}

	public List<ExtendElement> getNestedExtendList() {
		return nestedExtendList;
	}

	public List<ExtensionsElement> getExtensionsList() {
		return extensionsList;
	}

	public boolean isTagReserved(int tag) {
		return reserveds.stream().anyMatch(reserved -> reserved.matchesTag(tag));
	}

	public boolean isNameReserved(String fieldName) {
		return reserveds.stream().anyMatch(reserved -> reserved.matchesName(fieldName));
	}

	public void addReserved(String documentation, Location location, int tag) {
		if (!isTagReserved(tag)) {
			reserveds.add(new Reserved(location, documentation == null ? "" : documentation, List.of(tag)));
		}
	}

	public void addReserved(String documentation, Location location, String fieldName) {
		if (!isNameReserved(fieldName)) {
			reserveds.add(new Reserved(location, documentation == null ? "" : documentation, List.of(fieldName)));
		}
	}

	public int getNextFieldNum() {
		fieldNum++;
		return fieldNum;
	}

	public void advanceFieldNum() {
		if (fieldNum == 0) {
			return;
		}
		int newFieldNum = (fieldNum + FIELD_NUM_INCREMENT) - (fieldNum % FIELD_NUM_INCREMENT);
		if (newFieldNum - (FIELD_NUM_INCREMENT / 3) < fieldNum) {
			fieldNum = newFieldNum;
			advanceFieldNum();
		} else {
			fieldNum = newFieldNum;
		}
	}

	public String getName() {
		return name;
	}

	public void updateName(String newName) {
		this.name = newName;
		protoType = ProtoType.get(protoType.getEnclosingTypeOrPackage(), newName);
	}

	@Override
	public ProtoType type() {
		return protoType;
	}

	@Override
	public Location location() {
		return location;
	}

	@Override
	public String documentation() {
		return documentation;
	}

	@Override
	public void updateDocumentation(String documentation) {
		this.documentation = documentation;
	}

	@Override
	public MutableOptions options() {
		return options;
	}

	@Override
	public List<MutableType> nestedTypes() {
		return nestedTypes;
	}

	public void addField(MutableField f) {
		declaredFields.add(f);
	}

	public void setDeclaredFields(List<MutableField> newFields) {
		this.declaredFields = newFields;
	}

	/** Mirrors the vendored behaviour: returns a fresh combined list (so sorting it does not reorder declared fields). */
	public List<MutableField> fields() {
		return new ArrayList<>(declaredFields);
	}

	public void removeDeclaredField(MutableField f) {
		declaredFields.remove(f);
	}

	public List<MutableField> fieldsAndOneOfFields() {
		List<MutableField> result = new ArrayList<>(declaredFields);
		for (MutableOneOf oneOf : oneOfs) {
			result.addAll(oneOf.fields());
		}
		return result;
	}

	public MutableField field(String name) {
		for (MutableField field : declaredFields) {
			if (field.name().equals(name)) {
				return field;
			}
		}
		for (MutableOneOf oneOf : oneOfs) {
			for (MutableField field : oneOf.fields()) {
				if (field.name().equals(name)) {
					return field;
				}
			}
		}
		return null;
	}

	public List<MutableOneOf> oneOfs() {
		return oneOfs;
	}

	public void removeOneOf(MutableOneOf oneOfToRemove) {
		oneOfs.remove(oneOfToRemove);
	}

	void setSourceElement(MessageElement sourceElement) {
		this.sourceElement = sourceElement;
	}

	@Override
	public MessageElement toElement() {
		// Stock wire serializes message fields sorted by Location (line, column), not by list order. Encode each field's list position
		// as its location so the vendored serializer's list-order output (declared fields first, then oneOfs) is reproduced exactly.
		AtomicInteger order = new AtomicInteger(0);
		List<FieldElement> fieldElements = declaredFields.stream().map(f -> f.toElement(order.getAndIncrement())).collect(Collectors.toList());
		List<OneOfElement> oneOfElements = oneOfs.stream().map(o -> o.toElement(order)).collect(Collectors.toList());
		List<TypeElement> nestedTypeElements = nestedTypes.stream().map(MutableType::toElement).collect(Collectors.toList());
		String doc = documentation == null ? "" : documentation;
		if (sourceElement != null) {
			return sourceElement.copy(location, name, doc, nestedTypeElements, options.toElements(), Reserved.toElements(reserveds), fieldElements,
					oneOfElements, new ArrayList<>(extensionsList), sourceElement.getGroups(), new ArrayList<>(nestedExtendList));
		}
		return new MessageElement(location, name, doc, nestedTypeElements, options.toElements(), Reserved.toElements(reserveds), fieldElements, oneOfElements,
				new ArrayList<>(extensionsList), List.of(), new ArrayList<>(nestedExtendList));
	}

	@Override
	public String toString() {
		return "MessageType [name=" + name + "]";
	}
}
