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

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.ProtoFile;

// Combine field.packageName and field.Typename to field.packageName.typeName
public class MoveFieldPackageNameToFieldTypeName implements Processor {
	@Override
	public void process(Map<String, ProtoFile> packageToProtoFileMap) {
		packageToProtoFileMap.values().stream().flatMap(f -> messageTypes(f.types())).forEach(this::moveFieldPackageNameToFieldTypeName);
	}

	// Recursively loops through all fields for all nested types
	private void moveFieldPackageNameToFieldTypeName(MessageType messageType) {
		messageTypes(messageType.nestedTypes()).forEach(this::moveFieldPackageNameToFieldTypeName);

		for (Field field : messageType.fieldsAndOneOfFields()) {
			String fieldPackageName = StringUtils.trimToNull(field.packageName());
			if (fieldPackageName != null) {
				field.clearPackageName();
				field.updateElementType(fieldPackageName + "." + field.getElementType());
			}
		}
	}
}
