package no.entur.schema2proto.generateproto.serializer;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squareup.wire.schema.ProtoFile;

public class ResolveRecursiveImports implements Processor {
	private static Logger LOGGER = LoggerFactory.getLogger(ResolveRecursiveImports.class);

	@Override
	public void process(Map<String, ProtoFile> packageToProtoFileMap) {
		Map<String, List<String>> imports = new HashMap<>();
		for (ProtoFile file : packageToProtoFileMap.values()) {

			List<String> fileImports = new ArrayList<>(file.imports());
			for (int i = 0; i < fileImports.size(); i++) {
				// Removing path-info from fileimport
				fileImports.set(i, fileImports.get(i).substring(fileImports.get(i).lastIndexOf("/") + 1));
			}
			imports.put(file.toString(), fileImports);
		}

		for (Map.Entry<String, ProtoFile> protoFileEntry : packageToProtoFileMap.entrySet()) {
			ProtoFile protoFile = protoFileEntry.getValue();
			String filename = protoFile.location().getPath();
			if (hasRecursiveImports(imports, filename, filename)) {
				LOGGER.error("File {} recursively imports itself.", filename);
				// TODO: Extract affected types to a separate, common ProtoFile
			}
		}
	}

	private boolean hasRecursiveImports(Map<String, List<String>> imports, String rootFilename, String filename) {
		if (imports.containsKey(filename)) {
			List<String> currentImports = imports.get(filename);
			if (currentImports.contains(rootFilename)) {
				return true;
			}
			for (String currImport : currentImports) {
				if (hasRecursiveImports(imports, rootFilename, currImport)) {
					return true;
				}
			}
		}
		return false;
	}
}
