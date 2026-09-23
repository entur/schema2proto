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

import com.squareup.wire.schema.Field.Label;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.internal.parser.FieldElement;

/**
 * Mutable builder analogue of {@link com.squareup.wire.schema.Field}.
 *
 * <p>
 * {@code packageName} is schema2proto's transient notion of the proto package of the field's referenced type (used to compute imports); it is folded into
 * {@code elementType} before serialization. Wire's own {@code FieldElement} carries no such concept, so it is not emitted.
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
	// Explicit json_name of the source field, if any. Never set by the XSD-to-proto path; carried through unchanged when modifying existing protos.
	private String jsonName;

	private boolean fromElement;
	private boolean fromAttribute;

	/** The element this field was built from, if any. See {@link MutableType#toElement()}. */
	private FieldElement sourceElement;

	public MutableField(String packageName, Location location, Label label, String name, String documentation, int tag, String elementType,
			MutableOptions options, boolean fromElement) {
		this(packageName, location, label, name, documentation, tag, null, elementType, options, false, fromElement);
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

	public String jsonName() {
		return jsonName;
	}

	public void setJsonName(String jsonName) {
		this.jsonName = jsonName;
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

	void setSourceElement(FieldElement sourceElement) {
		this.sourceElement = sourceElement;
	}

	/**
	 * @param order the field's position within its enclosing message. Stock wire serializes message fields sorted by their {@link Location} (line, column)
	 *              rather than by list order, so we encode the intended emission order as the location line to reproduce the vendored serializer's list-order
	 *              output.
	 */
	public FieldElement toElement(int order) {
		Location orderedLocation = new Location("", "", order, 0);
		String doc = documentation == null ? "" : documentation;
		if (sourceElement != null) {
			return sourceElement.copy(orderedLocation, label, elementType, name, defaultValue, jsonName, tag, doc, options.toElements());
		}
		return new FieldElement(orderedLocation, label, elementType, name, defaultValue, jsonName, tag, doc, options.toElements());
	}

	@Override
	public String toString() {
		return "Field{name=" + name + ", tag=" + tag + ", type=" + elementType + "}";
	}
}
