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

import static no.entur.schema2proto.generateproto.serializer.CommonUtils.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squareup.wire.schema.EnumType;
import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.Type;

import no.entur.schema2proto.generateproto.SchemaParser;

public class ReplaceGeneratedSuffix implements Processor {
	private static Logger LOGGER = LoggerFactory.getLogger(ReplaceGeneratedSuffix.class);

	@Override
	public void process(Map<String, ProtoFile> packageToProtoFileMap) {
		packageToProtoFileMap.values()
				.forEach(file -> replaceGeneratedSuffix(packageToProtoFileMap, SchemaParser.GENERATED_NAME_SUFFIX_UNIQUENESS, SchemaParser.TYPE_SUFFIX,
						file.types(), file.packageName()));
	}

	private void replaceGeneratedSuffix(Map<String, ProtoFile> packageToProtoFileMap, String generatedRandomTypeSuffix, String newTypeSuffix, List<Type> types,
			String packageName) {
		Set<String> usedNames = findExistingTypeNamesInProtoFile(types);
		for (Type type : types) {
			// Recurse into nested types
			replaceGeneratedSuffix(packageToProtoFileMap, generatedRandomTypeSuffix, newTypeSuffix, type.nestedTypes(), packageName);

			if (type instanceof MessageType) {
				MessageType mt = (MessageType) type;
				messageTypes(mt.nestedTypes())
						.forEach(e -> replaceGeneratedSuffix(packageToProtoFileMap, generatedRandomTypeSuffix, newTypeSuffix, packageName, usedNames, e));

				replaceGeneratedSuffix(packageToProtoFileMap, generatedRandomTypeSuffix, newTypeSuffix, packageName, usedNames, mt);

			} else if (type instanceof EnumType) {
				EnumType et = (EnumType) type;
				String messageName = et.name();
				if (messageName.endsWith(generatedRandomTypeSuffix)) {
					String newMessageName = messageName.replace(generatedRandomTypeSuffix, newTypeSuffix);
					if (!usedNames.contains(newMessageName)) {
						et.updateName(newMessageName);
						usedNames.add(newMessageName);
						updateTypeReferences(packageToProtoFileMap, packageName, messageName, newMessageName);
					} else {
						LOGGER.warn("Cannot rename enum " + messageName + " to " + newMessageName + " as type already exist! Renaming ignored");
					}
				}
			}
		}
	}

	private void replaceGeneratedSuffix(Map<String, ProtoFile> packageToProtoFileMap, String generatedRandomTypeSuffix, String newTypeSuffix,
			String packageName, Set<String> usedNames, MessageType mt) {
		String messageName = mt.getName();
		if (messageName.endsWith(generatedRandomTypeSuffix)) {
			String newMessageName = messageName.replace(generatedRandomTypeSuffix, newTypeSuffix);
			if (!usedNames.contains(newMessageName)) {
				mt.updateName(newMessageName);
				usedNames.add(newMessageName);
				updateTypeReferences(packageToProtoFileMap, packageName, messageName, newMessageName);
			} else {
				LOGGER.warn("Cannot rename message " + messageName + " to " + newMessageName + " as type already exist! Renaming ignored");
			}
		}
	}
}
