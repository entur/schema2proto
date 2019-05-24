package no.entur.schema2proto;

import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.sun.xml.xsom.XmlString;

public class Field {

	@Override
	public String toString() {
		return "Field [name=" + name + ", typeNamespace=" + typeNamespace + ", type=" + type + ", required=" + required + ", repeat=" + repeat + ", def=" + def
				+ "]";
	}

	private String name;
	private String typeNamespace;
	private String type;
	private String doc;
	private boolean required;
	private boolean repeat;
	private XmlString def;

	public Field(String name, String typeNamespace, String type, boolean repeat, XmlString def, String doc, boolean required) {
		this.name = name;
		this.type = type;
		this.required = required;
		this.def = def;
		this.repeat = repeat;
		this.doc = doc;
		this.typeNamespace = typeNamespace;
	}

	public String getTypeNamespace() {
		return typeNamespace;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDoc() {
		return doc;
	}

	public void setDoc(String doc) {
		this.doc = doc;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public boolean isRepeat() {
		return repeat;
	}

	public void setRepeat(boolean repeat) {
		this.repeat = repeat;
	}

	public XmlString getDef() {
		return def;
	}

	public void setDef(XmlString def) {
		this.def = def;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		Field field = (Field) o;

		return new org.apache.commons.lang3.builder.EqualsBuilder().append(required, field.required)
				.append(repeat, field.repeat)
				.append(name, field.name)
				.append(typeNamespace, field.typeNamespace)
				.append(type, field.type)
				.append(doc, field.doc)
				.append(def, field.def)
				.isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).append(name).append(typeNamespace).append(type).append(doc).append(required).append(repeat).append(def).toHashCode();
	}
}
