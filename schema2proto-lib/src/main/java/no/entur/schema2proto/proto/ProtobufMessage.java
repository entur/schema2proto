package no.entur.schema2proto.proto;

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

import java.util.*;

import com.sun.xml.xsom.XmlString;

import no.entur.schema2proto.Field;
import no.entur.schema2proto.NamespaceConverter;

public class ProtobufMessage implements Comparable<ProtobufMessage> {
	private Map<String, Field> map;
	private Set<String> types;
	private List<Field> orderedFields;
	private String name;
	private String namespace;
	private String parent;
	private String doc;

	public ProtobufMessage(String name, String namespace) {
		this.name = name;
		this.namespace = namespace;
		map = new TreeMap<String, Field>();
		types = new TreeSet<String>();
		orderedFields = new LinkedList<Field>();
	}

	public void addFields(List<Field> fields, HashMap<String, String> xsdMapping) {
		for (Field field : fields) {
			addField(field.getName(), field.getTypeNamespace(), field.getType(), field.isRequired(), field.isRepeat(), field.getDef(), field.getDoc(),
					xsdMapping);
		}
	}

	public void addField(String name, String type, boolean required, boolean repeat, XmlString def, String doc, Map<String, String> xsdMapping) {
		addField(name, null, type, required, repeat, def, doc, xsdMapping);
	}

	public void addField(String name, String namespace, String type, boolean required, boolean repeat, XmlString def, String doc,
			Map<String, String> xsdMapping) {
		Field f;

		if (map.get(name) == null) {
			if (type == null) {
				type = new String(name);
			} else {
				if (xsdMapping.containsKey(type)) {
					type = xsdMapping.get(type);
				} else if (type.equals(this.name)) {
					type = "binary";
				}
			}
			f = new Field(name, NamespaceConverter.convertFromSchema(namespace), type, repeat, def, doc, required);
			map.put(name, f);
			orderedFields.add(f);
			if (!type.equals(this.name)) {
				types.add(type);
			}
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDoc() {
		return doc;
	}

	public void setDoc(String doc) {
		this.doc = doc;
	}

	public List<Field> getFields() {
		return orderedFields;
	}

	public Collection<String> getTypes() {
		return types;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public String getParent() {
		return parent;
	}

	public String toString() {
		return "ProtobufMessage[name=" + name + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		ProtobufMessage other = (ProtobufMessage) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public String getNamespace() {
		return namespace;
	}

	@Override
	public int compareTo(ProtobufMessage s) {
		return name.compareTo(s.name);
	}

}
