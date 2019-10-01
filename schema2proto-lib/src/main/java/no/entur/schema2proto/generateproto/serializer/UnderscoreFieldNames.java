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

import static no.entur.schema2proto.generateproto.serializer.CommonUtils.UNDERSCORE;
import static no.entur.schema2proto.generateproto.serializer.CommonUtils.messageTypes;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.CaseFormat;
import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.ProtoFile;

public class UnderscoreFieldNames implements Processor {
	private static final String DASH = "-";

	@Override
	public void process(Map<String, ProtoFile> packageToProtoFileMap) {
		packageToProtoFileMap.values().stream().flatMap(file -> messageTypes(file.types())).forEach(this::underscoreFieldNames);
	}

	private void underscoreFieldNames(MessageType mt) {

		messageTypes(mt.nestedTypes()).forEach(this::underscoreFieldNames);

		for (Field field : mt.fieldsAndOneOfFields()) {
			String fieldName = field.name();
			boolean startsWithUnderscore = fieldName.startsWith(UNDERSCORE);
			boolean endsWithUnderscore = fieldName.endsWith(UNDERSCORE);

			String strippedFieldName = StringUtils.removeEnd(StringUtils.removeStart(fieldName, UNDERSCORE), UNDERSCORE);

			String newFieldName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, strippedFieldName);

			// Remove all dashes
			newFieldName = StringUtils.remove(newFieldName, DASH);

			if (endsWithUnderscore) {
				newFieldName += "u"; // Trailing underscore not accepted by protoc for java
			}

			if (startsWithUnderscore) {
				newFieldName = UNDERSCORE + newFieldName;
			}

			field.updateName(newFieldName);
		}

	}
}
