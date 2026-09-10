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

import java.util.Collections;
import java.util.List;

import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.Field.Label;
import com.squareup.wire.schema.Location;

/**
 * Mutable builder analogue of {@link com.squareup.wire.schema.Field}.
 *
 * <p>
 * {@code packageName} is schema2proto's transient notion of the proto package of the field's referenced type (used to compute imports); it is folded into
 * {@code elementType} before serialization. The stock wire {@code Field} carries no such concept (its {@code namespaces} are the enclosing scope, irrelevant
 * for canonical serialization), so {@link #toWire()} emits an empty namespaces list.
 */
public class MutableField {

	private String packageName;
	private final Location location;
	private Label label;
	private String name;
	private String documentation;
	private int tag;
	private final String defaultValue;
	private String elementType;
	private final MutableOptions options;
	private final boolean extension;
	private boolean isOneOf;

	private boolean fromElement;
	private boolean fromAttribute;

	public MutableField(String packageName, Location location, Label label, String name, String documentation, int tag, String elementType,
			MutableOptions options, boolean extension) {
		this(packageName, location, label, name, documentation, tag, null, elementType, options, extension, false);
	}

	public MutableField(String packageName, Location location, Label label, String name, String documentation, int tag, String defaultValue, String elementType,
			MutableOptions options, boolean extension, boolean fromElement) {
		this.packageName = packageName;
		this.location = location;
		this.label = label;
		this.name = name;
		this.documentation = documentation;
		this.tag = tag;
		this.defaultValue = defaultValue;
		this.elementType = elementType;
		this.options = options;
		this.extension = extension;
		this.fromElement = fromElement;
	}

	public String name() {
		return name;
	}

	public void updateName(String newFieldName) {
		this.name = newFieldName;
	}

	public String getElementType() {
		return elementType;
	}

	public void updateElementType(String newFieldType) {
		this.elementType = newFieldType;
	}

	public int tag() {
		return tag;
	}

	public void updateTag(int updatedTag) {
		this.tag = updatedTag;
	}

	public Label label() {
		return label;
	}

	public void setLabel(Label label) {
		this.label = label;
	}

	public String documentation() {
		return documentation;
	}

	public void updateDocumentation(String newDocumentation) {
		this.documentation = newDocumentation;
	}

	public String packageName() {
		return packageName;
	}

	public void updatePackageName(String newPackageName) {
		this.packageName = newPackageName;
	}

	public void clearPackageName() {
		this.packageName = null;
	}

	public MutableOptions options() {
		return options;
	}

	public Location location() {
		return location;
	}

	public boolean isExtension() {
		return extension;
	}

	public boolean isFromAttribute() {
		return fromAttribute;
	}

	public void setFromAttribute(boolean fromAttribute) {
		this.fromAttribute = fromAttribute;
	}

	public boolean isFromElement() {
		return fromElement;
	}

	public void setFromElement(boolean fromElement) {
		this.fromElement = fromElement;
	}

	void setOneOf(boolean isOneOf) {
		this.isOneOf = isOneOf;
	}

	/**
	 * @param order the field's position within its enclosing message. Stock wire serializes message fields sorted by their {@link Location} (line, column)
	 *              rather than by list order, so we encode the intended emission order as the location line to reproduce the vendored serializer's list-order
	 *              output.
	 */
	public Field toWire(int order) {
		List<String> namespaces = Collections.emptyList();
		Location orderedLocation = new Location("", "", order, 0);
		return new Field(namespaces, orderedLocation, label, name, documentation == null ? "" : documentation, tag, defaultValue, elementType, options.toWire(),
				extension, isOneOf, null);
	}

	@Override
	public String toString() {
		return "Field{name=" + name + ", tag=" + tag + ", type=" + elementType + "}";
	}
}
