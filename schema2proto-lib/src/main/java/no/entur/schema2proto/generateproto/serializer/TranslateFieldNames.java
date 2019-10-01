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

import java.util.List;
import java.util.Map;

import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.ProtoFile;

import no.entur.schema2proto.generateproto.TypeAndNameMapper;

public class TranslateFieldNames implements Processor {
	private TypeAndNameMapper typeAndFieldNameMapper;

	public TranslateFieldNames(TypeAndNameMapper typeAndFieldNameMapper) {
		this.typeAndFieldNameMapper = typeAndFieldNameMapper;
	}

	@Override
	public void process(Map<String, ProtoFile> packageToProtoFileMap) {
		packageToProtoFileMap.values().stream().flatMap(f -> messageTypes(f.types())).forEach(messageType -> {
			messageTypes(messageType.nestedTypes()).forEach(nested -> translateFieldNames(nested.fieldsAndOneOfFields()));
			translateFieldNames(messageType.fieldsAndOneOfFields());
		});

	}

	private void translateFieldNames(List<Field> fields) {
		for (Field field : fields) {
			String fieldName = field.name();
			String newFieldName = typeAndFieldNameMapper.translateFieldName(fieldName);
			field.updateName(newFieldName);
		}
	}
}
