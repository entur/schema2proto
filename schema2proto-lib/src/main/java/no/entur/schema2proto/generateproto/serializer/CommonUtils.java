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

import java.util.*;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import com.squareup.wire.schema.*;

public class CommonUtils {
	public static final String UNDERSCORE = "_";

	@NotNull
	public static String getPathFromPackageName(String packageName) {
		return packageName.replace('.', '/');
	}

	public static Set<String> findExistingTypeNamesInProtoFile(List<Type> types) {
		Set<String> existingTypeNames = new HashSet<>();
		for (Type t : types) {
			if (t instanceof MessageType) {
				existingTypeNames.add(((MessageType) t).getName());
			} else if (t instanceof EnumType) {
				existingTypeNames.add(((EnumType) t).name());
			}
		}

		return existingTypeNames;
	}

	public static void updateTypeReferences(Map<String, ProtoFile> packageToProtoFileMap, String packageNameOfType, String oldName, String newName) {
		for (ProtoFile file : packageToProtoFileMap.values()) {
			updateTypeReferences(packageNameOfType, oldName, newName, file.types());
		}

	}

	private static void updateTypeReferences(String packageNameOfType, String oldName, String newName, List<Type> types) {
		for (Type type : types) {
			updateTypeReferences(packageNameOfType, oldName, newName, type.nestedTypes());

			if (type instanceof MessageType) {
				MessageType mt = (MessageType) type;

				mt.nestedTypes()
						.stream()
						.filter(e -> e instanceof MessageType)
						.forEach(e -> updateTypeReferences(packageNameOfType, oldName, newName, (MessageType) e, ((MessageType) e).fieldsAndOneOfFields()));
				updateTypeReferences(packageNameOfType, oldName, newName, mt, mt.fieldsAndOneOfFields());
			}
		}
	}

	private static void updateTypeReferences(String packageNameOfType, String oldName, String newName, MessageType mt, Collection<Field> fields) {
		for (Field field : fields) {
			if (Objects.equals(field.packageName(), packageNameOfType)) {
				String fieldType = field.getElementType();
				if (fieldType.equals(oldName)) {
					field.updateElementType(newName);
				}
			}
		}
	}

	static Stream<MessageType> messageTypes(Collection<Type> types) {
		return types.stream().filter(t -> t instanceof MessageType).map(t -> (MessageType) t);
	}
}
