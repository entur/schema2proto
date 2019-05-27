
package no.entur.schema2proto;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

import no.entur.schema2proto.marshal.ProtobufMarshaller;

public class OutputWriter {

	private String filename, directory;
	private boolean splitBySchema;
	private OutputStream os;
	private Map<String, OutputStream> streams;
	private ProtobufMarshaller marshaller;
	private String defaultNamespace;
	private String defaultExtension;
	Map<String, Set<String>> inclusions = null;

	public void setDefaultExtension(String defaultExtension) {
		this.defaultExtension = defaultExtension;
	}

	public OutputStream getStream(String ns) throws IOException {
		if (os == null && streams == null) {
			initializeOutputStream();
		}
		if (os != null)
			return os;
		if (ns == null)
			ns = defaultNamespace;

		if (ns == null)
			ns = "default";

		return getNamespaceSpecificStream(ns);
	}

	private OutputStream getNamespaceSpecificStream(String cleanedNamespace) throws IOException {
		if (!streams.containsKey(cleanedNamespace)) {
			OutputStream os = new FileOutputStream(directory() + cleanedNamespace.replace(".", "_") + "." + defaultExtension);
			streams.put(cleanedNamespace, os);
			os.write(marshaller.writeHeader(cleanedNamespace).getBytes());
		}
		return streams.get(cleanedNamespace);
	}

	private void initializeOutputStream() throws IOException {
		if (splitBySchema) {
			streams = new HashMap<String, OutputStream>();
		} else {
			if (filename == null) {
				os = System.out;
				os.write(marshaller.writeHeader(defaultNamespace).getBytes());
			} else {
				os = new FileOutputStream(directory() + filename);
				os.write(marshaller.writeHeader(defaultNamespace).getBytes());
			}
		}
	}

	public void setDefaultNamespace(String defaultNamespace) {
		this.defaultNamespace = defaultNamespace;
	}

	private String directory() {
		return directory == null ? "" : directory + "/";
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public void setDirectory(String directory) {
		File f = new File(directory);
		if (!f.exists()) {
			f.mkdirs();
		}

		this.directory = directory;
	}

	public void setSplitBySchema(boolean splitBySchema) {
		this.splitBySchema = splitBySchema;
	}

	public boolean isSplitBySchema() {
		return splitBySchema;
	}

	public void setMarshaller(ProtobufMarshaller marshaller) {
		this.marshaller = marshaller;
	}

	public void addInclusion(String namespace, String includeNamespace) {
		if (inclusions == null)
			inclusions = new HashMap<String, Set<String>>();

		if (!inclusions.containsKey(namespace))
			inclusions.put(namespace, new TreeSet<String>());

		inclusions.get(namespace).add(includeNamespace);
	}

	public void postProcessNamespacedFilesForIncludes() throws IOException {

		if (streams != null) {
			Iterator<OutputStream> i = streams.values().iterator();
			while (i.hasNext()) {
				OutputStream o = i.next();
				o.flush();
				o.close();
			}
			if (inclusions != null) {
				Iterator<String> namespaces = inclusions.keySet().iterator();
				while (namespaces.hasNext()) {
					String namespace = namespaces.next();
					File f = new File(directory() + namespace.replace(".", "_") + "." + defaultExtension);
					if (f.exists()) {
						writeIncludes(f, inclusions.get(namespace));
					}
				}
			}
		} else {
			if (inclusions != null && !marshaller.imports.isEmpty()) {
				Iterator<String> namespaces = inclusions.keySet().iterator();
				Set<String> requiredImports = new HashSet<>();
				while (namespaces.hasNext()) {
					requiredImports.addAll(inclusions.get(namespaces.next()));

				}
				requiredImports.retainAll(marshaller.imports.values());

				File f = new File(filename);
				if (f.exists()) {
					writeIncludes(f, marshaller.imports.values());
				}
			}
		}
	}

	private void writeIncludes(File f, Collection<String> toInclude) throws IOException {
		Iterator<String> i = toInclude.iterator();
		BufferedReader reader = new BufferedReader(new FileReader(f));
		String line = null;
		StringBuffer output = new StringBuffer();
		int count = 0;
		while ((line = reader.readLine()) != null) {
			output.append(line + "\n");
			if (count == 1) {
				while (i.hasNext()) {
					output.append(marshaller.writeInclude(i.next().replace(".", "_")));
				}
				output.append("\n");
			}
			count++;
		}
		reader.close();
		FileWriter writer = new FileWriter(f);
		writer.append(output.toString());
		writer.flush();
		writer.close();
	}
}
