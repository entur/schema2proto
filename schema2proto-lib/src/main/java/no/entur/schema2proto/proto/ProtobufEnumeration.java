
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

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class ProtobufEnumeration {
	private String name;
	private Set<String> strings;
	private String namespace;
	private String doc;

	public ProtobufEnumeration(String name, String namespace) {
		this.setName(name);
		this.namespace = namespace;
		this.strings = new TreeSet<String>();
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void addString(String value) {
		strings.add(value);
	}

	public Iterator<String> iterator() {
		return strings.iterator();
	}

	public String getNamespace() {
		return namespace;
	}

	public String getDoc() {
		return doc;
	}

	public void setDoc(String doc) {
		this.doc = doc;
	}
}
