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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squareup.wire.schema.*;

import no.entur.schema2proto.generateproto.TypeAndNameMapper;

public class TranslateTypes implements Processor {
	private static Logger LOGGER = LoggerFactory.getLogger(TranslateTypes.class);
	private TypeAndNameMapper typeAndFieldNameMapper;
	private Set<String> basicTypes;

	public TranslateTypes(TypeAndNameMapper typeAndFieldNameMapper, Set<String> basicTypes) {
		this.typeAndFieldNameMapper = typeAndFieldNameMapper;
		this.basicTypes = basicTypes;
	}

	@Override
	public void process(Map<String, ProtoFile> packageToProtoFileMap) {
		packageToProtoFileMap.values().forEach(file -> translateTypes(packageToProtoFileMap, file.types(), file.packageName()));
	}

	private void translateTypes(Map<String, ProtoFile> packageToProtoFileMap, List<Type> types, String packageName) {
		if (types.size() > 0) {
			Set<String> usedNames = findExistingTypeNamesInProtoFile(types);
			for (Type type : types) {
				if (type instanceof MessageType) {
					MessageType mt = (MessageType) type;

					translateTypes(packageToProtoFileMap, type.nestedTypes(), packageName);

					String messageName = mt.getName();
					String newMessageName = typeAndFieldNameMapper.translateType(messageName);

					if (!messageName.equals(newMessageName)) {
						if (!usedNames.contains(newMessageName)) {
							mt.updateName(newMessageName);
							usedNames.add(newMessageName);
							updateTypeReferences(packageToProtoFileMap, packageName, messageName, newMessageName);
						} else {
							LOGGER.warn("Cannot rename message " + messageName + " to " + newMessageName + " as type already exist! Renaming ignored");
						}

					}
					translateTypes(mt.fieldsAndOneOfFields());

				} else if (type instanceof EnumType) {
					EnumType et = (EnumType) type;
					String messageName = et.name();
					String newMessageName = typeAndFieldNameMapper.translateType(messageName);

					if (!messageName.equals(newMessageName)) {
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
	}

	private void translateTypes(List<Field> fields) {
		for (Field field : fields) {
			// Translate basic types as well
			if (field.packageName() == null && basicTypes.contains(field.getElementType())) {
				String newFieldType = typeAndFieldNameMapper.translateType(field.getElementType());
				if (!newFieldType.equals(field.getElementType())) {
					LOGGER.debug("Replacing basicType " + field.getElementType() + " with " + newFieldType);
					field.updateElementType(newFieldType);
				}
			}

		}
	}
}
