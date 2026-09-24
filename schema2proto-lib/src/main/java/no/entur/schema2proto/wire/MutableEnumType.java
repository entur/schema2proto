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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.ProtoType;
import com.squareup.wire.schema.Reserved;
import com.squareup.wire.schema.internal.parser.EnumConstantElement;
import com.squareup.wire.schema.internal.parser.EnumElement;

/** Mutable builder analogue of {@link com.squareup.wire.schema.EnumType}. */
public class MutableEnumType extends MutableType {

	private ProtoType protoType;
	private final Location location;
	private String documentation;
	private String name;
	private final List<MutableEnumConstant> constants;
	private final List<Reserved> reserveds;
	private final MutableOptions options;
	/** The element this enum was built from, if any. See {@link MutableType#toElement()}. */
	private EnumElement sourceElement;

	public MutableEnumType(ProtoType protoType, Location location, String documentation, String name, List<MutableEnumConstant> constants,
			List<Reserved> reserveds, MutableOptions options) {
		this.protoType = protoType;
		this.location = location;
		this.documentation = documentation;
		this.name = name;
		this.constants = constants != null ? constants : new ArrayList<>();
		this.reserveds = reserveds != null ? reserveds : new ArrayList<>();
		this.options = options;
	}

	public String name() {
		return name;
	}

	public void updateName(String newName) {
		this.name = newName;
		protoType = ProtoType.get(protoType.getEnclosingTypeOrPackage(), newName);
	}

	public List<MutableEnumConstant> constants() {
		return constants;
	}

	public List<Reserved> getReserveds() {
		return reserveds;
	}

	public List<Reserved> reserveds() {
		return reserveds;
	}

	public boolean isTagReserved(int tag) {
		return reserveds.stream().anyMatch(reserved -> reserved.matchesTag(tag));
	}

	public boolean isNameReserved(String constantName) {
		return reserveds.stream().anyMatch(reserved -> reserved.matchesName(constantName));
	}

	public void addReserved(String documentation, Location location, int tag) {
		if (!isTagReserved(tag)) {
			reserveds.add(new Reserved(location, documentation == null ? "" : documentation, List.of(tag)));
		}
	}

	public void addReserved(String documentation, Location location, String constantName) {
		if (!isNameReserved(constantName)) {
			reserveds.add(new Reserved(location, documentation == null ? "" : documentation, Collections.singletonList(constantName)));
		}
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
		return Collections.emptyList();
	}

	void setSourceElement(EnumElement sourceElement) {
		this.sourceElement = sourceElement;
	}

	@Override
	public EnumElement toElement() {
		List<EnumConstantElement> constantElements = constants.stream().map(MutableEnumConstant::toElement).collect(Collectors.toList());
		String doc = documentation == null ? "" : documentation;
		if (sourceElement != null) {
			return sourceElement.copy(location, name, doc, options.toElements(), constantElements, Reserved.toElements(reserveds));
		}
		return new EnumElement(location, name, doc, options.toElements(), constantElements, Reserved.toElements(reserveds));
	}

	@Override
	public String toString() {
		return "EnumType [name=" + name + "]";
	}
}
