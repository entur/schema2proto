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

import static no.entur.schema2proto.generateproto.serializer.CommonUtils.findExistingTypeNamesInProtoFile;
import static no.entur.schema2proto.generateproto.serializer.CommonUtils.updateTypeReferences;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squareup.wire.schema.EnumType;
import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.Type;

public class UppercaseMessageNames implements Processor {
	private static Logger LOGGER = LoggerFactory.getLogger(UppercaseMessageNames.class);

	@Override
	public void process(Map<String, ProtoFile> packageToProtoFileMap) {
		for (ProtoFile file : packageToProtoFileMap.values()) {
			uppercaseMessageNames(packageToProtoFileMap, file.types(), file.packageName());
		}

	}

	private void uppercaseMessageNames(Map<String, ProtoFile> packageToProtoFileMap, List<Type> types, String packageName) {
		Set<String> usedNames = findExistingTypeNamesInProtoFile(types);
		for (Type type : types) {
			// Recurse into nested types
			uppercaseMessageNames(packageToProtoFileMap, type.nestedTypes(), packageName);

			if (type instanceof MessageType) {
				MessageType mt = (MessageType) type;

				uppercaseMessageNames(packageToProtoFileMap, packageName, usedNames, mt);
			} else if (type instanceof EnumType) {
				EnumType et = (EnumType) type;
				String messageName = et.name();
				if (!Character.isUpperCase(messageName.charAt(0))) {
					String newMessageName = StringUtils.capitalize(messageName);
					if (!usedNames.contains(newMessageName)) {
						et.updateName(newMessageName);
						usedNames.add(newMessageName);
						updateTypeReferences(packageToProtoFileMap, packageName, messageName, newMessageName);
					} else {
						LOGGER.warn("Cannot uppercase enum " + messageName + " to " + newMessageName + " as type already exist! Renaming ignored");
					}
				}
			}
		}
	}

	private void uppercaseMessageNames(Map<String, ProtoFile> packageToProtoFileMap, String packageName, Set<String> usedNames, MessageType mt) {
		String messageName = mt.getName();
		if (!Character.isUpperCase(messageName.charAt(0))) {
			String newMessageName = StringUtils.capitalize(messageName);
			if (!usedNames.contains(newMessageName)) {
				mt.updateName(newMessageName);
				usedNames.add(newMessageName);
				updateTypeReferences(packageToProtoFileMap, packageName, messageName, newMessageName);
			} else {
				LOGGER.warn("Cannot uppercase message " + messageName + " to " + newMessageName + " as type already exist! Renaming ignored");
			}
		}
	}

}
