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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.sun.xml.xsom.XmlString;

import no.entur.schema2proto.NamespaceConverter;

public class ProtobufMessage implements Comparable<ProtobufMessage> {
	private Map<String, ProtobufField> fieldMap;
	private Set<String> types;
	private List<ProtobufField> orderedFields;
	private String messageName;
	private String packageName;
	private String doc;

	public ProtobufMessage(String name, String packageName) {
		this.messageName = name;
		this.packageName = packageName;
		fieldMap = new TreeMap<String, ProtobufField>();
		types = new TreeSet<String>();
		orderedFields = new LinkedList<ProtobufField>();
	}

	public void addFields(List<ProtobufField> fields, HashMap<String, String> xsdMapping) {
		for (ProtobufField field : fields) {
			addField(field.getName(), field.getTypePackage(), field.getType(), field.isRepeat(), field.getDef(), field.getDocumentation(), xsdMapping);
		}
	}

	public void addField(String name, String type, boolean repeat, XmlString def, String doc, Map<String, String> xsdMapping) {
		addField(name, null, type, repeat, def, doc, xsdMapping);
	}

	public void addField(String name, String namespace, String type, boolean repeat, XmlString def, String doc, Map<String, String> xsdMapping) {
		ProtobufField f;

		if (fieldMap.get(name) == null) {
			if (type == null) {
				type = new String(name);
			} else {
				if (xsdMapping.containsKey(type)) {
					type = xsdMapping.get(type);
				} else if (type.equals(this.messageName)) {
					type = "binary";
				}
			}
			f = new ProtobufField(name, NamespaceConverter.convertFromSchema(namespace), type, repeat, def, doc);
			fieldMap.put(name, f);
			orderedFields.add(f);
			if (!type.equals(this.messageName)) {
				types.add(type);
			}
		}
	}

	public String getMessageName() {
		return messageName;
	}

	public void setMessageName(String name) {
		this.messageName = name;
	}

	public String getDoc() {
		return doc;
	}

	public void setDoc(String doc) {
		this.doc = doc;
	}

	public List<ProtobufField> getFields() {
		return orderedFields;
	}

	public Collection<String> getTypes() {
		return types;
	}

	public String toString() {
		return "ProtobufMessage[messageName=" + messageName + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((messageName == null) ? 0 : messageName.hashCode());
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
		if (messageName == null) {
			if (other.messageName != null)
				return false;
		} else if (!messageName.equals(other.messageName))
			return false;
		return true;
	}

	public String getPackageName() {
		return packageName;
	}

	@Override
	public int compareTo(ProtobufMessage s) {
		return messageName.compareTo(s.messageName);
	}

}
