
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import no.entur.schema2proto.marshal.ProtobufMarshaller;

public class OutputWriter {

	private OutputStream outputStream;
	private Map<String, OutputStream> streams;
	private ProtobufMarshaller marshaller;
	private Schema2ProtoConfiguration configuration;
	private Map<String, Set<String>> inclusions = null;

	public OutputWriter(Schema2ProtoConfiguration configuration) {
		this.configuration = configuration;
		configuration.outputDirectory.mkdirs();
	}

	public OutputStream getStream(String protoPackage) throws IOException {
		if (outputStream == null && streams == null) {
			initializeOutputStream();
		}
		if (outputStream != null)
			return outputStream;
		if (protoPackage == null)
			protoPackage = configuration.defaultProtoPackage;

		if (protoPackage == null)
			protoPackage = "default";

		return getNamespaceSpecificStream(protoPackage);
	}

	private OutputStream getNamespaceSpecificStream(String cleanedNamespace) throws IOException {
		if (!streams.containsKey(cleanedNamespace)) {
			OutputStream os = new FileOutputStream(new File(configuration.outputDirectory, cleanedNamespace.replace(".", "_") + ".proto"));
			streams.put(cleanedNamespace, os);
			os.write(marshaller.writeHeader(cleanedNamespace).getBytes());
		}
		return streams.get(cleanedNamespace);
	}

	private void initializeOutputStream() throws IOException {
		if (configuration.splitBySchema) {
			streams = new HashMap<String, OutputStream>();
		} else {
			outputStream = new FileOutputStream(new File(configuration.outputDirectory, configuration.outputFilename));
			outputStream.write(marshaller.writeHeader(configuration.defaultProtoPackage).getBytes());
		}
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
					File f = new File(configuration.outputDirectory, namespace.replace(".", "_") + ".proto");
					if (f.exists()) {
						writeIncludes(f, inclusions.get(namespace));
					}
				}
			}
		} else {
			if (inclusions != null) {
				Iterator<String> namespaces = inclusions.keySet().iterator();
				Set<String> requiredImports = new HashSet<>();
				while (namespaces.hasNext()) {
					requiredImports.addAll(inclusions.get(namespaces.next()));

				}
				requiredImports.retainAll(marshaller.getImports().values());

				File f = new File(configuration.outputFilename);
				if (f.exists()) {
					writeIncludes(f, marshaller.getImports().values());
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
