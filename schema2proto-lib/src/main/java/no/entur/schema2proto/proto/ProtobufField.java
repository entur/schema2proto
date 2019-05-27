package no.entur.schema2proto.proto;

import com.sun.xml.xsom.XmlString;

public class ProtobufField {

	@Override
	public String toString() {
		return "ProtobufField [name=" + name + ", typePackage=" + typePackage + ", type=" + type + ", repeat=" + repeat + ", def=" + def + "]";
	}

	private String name;
	private String typePackage;
	private String type;
	private String documentation;
	private boolean repeat;
	private XmlString def;

	public ProtobufField(String name, String typeNamespace, String type, boolean repeat, XmlString def, String doc) {
		this.name = name;
		this.type = type;
		this.def = def;
		this.repeat = repeat;
		this.documentation = doc;
		this.typePackage = typeNamespace;
	}

	public String getTypePackage() {
		return typePackage;
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

	public String getDocumentation() {
		return documentation;
	}

	public void setDocumentation(String doc) {
		this.documentation = doc;
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
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((def == null) ? 0 : def.hashCode());
		result = prime * result + ((documentation == null) ? 0 : documentation.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + (repeat ? 1231 : 1237);
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((typePackage == null) ? 0 : typePackage.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProtobufField other = (ProtobufField) obj;
		if (def == null) {
			if (other.def != null)
				return false;
		} else if (!def.equals(other.def))
			return false;
		if (documentation == null) {
			if (other.documentation != null)
				return false;
		} else if (!documentation.equals(other.documentation))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (repeat != other.repeat)
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (typePackage == null) {
			if (other.typePackage != null)
				return false;
		} else if (!typePackage.equals(other.typePackage))
			return false;
		return true;
	}

}
