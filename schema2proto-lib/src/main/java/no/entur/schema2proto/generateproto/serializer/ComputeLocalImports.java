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

import static no.entur.schema2proto.generateproto.serializer.CommonUtils.getPathFromPackageName;
import static no.entur.schema2proto.generateproto.serializer.CommonUtils.messageTypes;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.ProtoFile;

public class ComputeLocalImports implements Processor {
	private static Logger LOGGER = LoggerFactory.getLogger(ComputeLocalImports.class);

	@Override
	public void process(Map<String, ProtoFile> packageToProtoFileMap) {
		packageToProtoFileMap.values().forEach(file -> {
			SortedSet<String> imports = new TreeSet<>(file.imports());
			messageTypes(file.types()).forEach(mt -> computeLocalImports(packageToProtoFileMap, file, imports, mt));

			file.imports().clear();
			file.imports().addAll(imports);

		});

	}

	private void computeLocalImports(Map<String, ProtoFile> packageToProtoFileMap, ProtoFile file, SortedSet<String> imports, MessageType messageType) {
		messageTypes(messageType.nestedTypes()).forEach(e -> computeLocalImports(packageToProtoFileMap, file, imports, e));

		for (Field field : messageType.fieldsAndOneOfFields()) {
			String packageName = StringUtils.trimToNull(field.packageName());
			if (file.packageName() != null && file.packageName().equals(packageName)) {
				field.clearPackageName();
			} else if (packageName != null) {
				// Add import
				ProtoFile fileToImport = packageToProtoFileMap.get(packageName);
				if (fileToImport != null) {
					imports.add(getPathFromPackageName(packageName) + "/" + fileToImport.location().getPath());
				} else {
					LOGGER.error("Tried to create import for field packageName " + packageName + ", but no such protofile exist");
				}
			}
		}
	}

}
