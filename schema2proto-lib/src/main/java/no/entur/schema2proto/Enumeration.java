
package no.entur.schema2proto;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class Enumeration {
	private String name;
	private Set<String> strings;
	private String namespace;
	private String doc;

	public Enumeration(String name, String namespace) {
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
