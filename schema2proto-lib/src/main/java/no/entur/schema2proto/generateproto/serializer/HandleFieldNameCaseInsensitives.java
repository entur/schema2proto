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

import static no.entur.schema2proto.generateproto.serializer.CommonUtils.messageTypes;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.ProtoFile;

// Handle cases where identical field name comes from both attribute and element (but with different case)
public class HandleFieldNameCaseInsensitives implements Processor {

	@Override
	public void process(Map<String, ProtoFile> packageToProtoFileMap) {
		packageToProtoFileMap.values().stream().flatMap(f -> messageTypes(f.types())).forEach(messageType -> {
			messageTypes(messageType.nestedTypes()).forEach(nested -> handleFieldNameCaseInsensitives(nested.fieldsAndOneOfFields()));
			handleFieldNameCaseInsensitives(messageType.fieldsAndOneOfFields());
		});
	}

	private void handleFieldNameCaseInsensitives(List<Field> fields) {
		Set<String> fieldNamesUppercase = new HashSet<>();

		for (Field field : fields) {
			String fieldName = field.name();
			boolean existedBefore = fieldNamesUppercase.add(fieldName.toUpperCase());
			if (!existedBefore) {
				fieldName = fieldName + CommonUtils.UNDERSCORE + "v"; // TODO handles only one duplicate, many can exist
				field.updateName(fieldName);
			}
		}
	}
}
