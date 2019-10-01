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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.squareup.wire.schema.*;

import no.entur.schema2proto.generateproto.TypeAndNameMapper;

public class RemoveUnwantedFields implements Processor {
	private TypeAndNameMapper typeAndFieldNameMapper;

	public RemoveUnwantedFields(TypeAndNameMapper typeAndFieldNameMapper) {
		this.typeAndFieldNameMapper = typeAndFieldNameMapper;
	}

	@Override
	public void process(Map<String, ProtoFile> packageToProtoFileMap) {

		for (ProtoFile file : packageToProtoFileMap.values()) {
			messageTypes(file.types()).forEach(messageType -> {
				messageTypes(messageType.nestedTypes()).forEach(nested -> removeUnwantedFields(file, nested));
				removeUnwantedFields(file, messageType);
			});
		}

	}

	private void removeUnwantedFields(ProtoFile file, MessageType mt) {
		List<Field> fieldsToRemove = getFieldsToRemove(file.packageName(), mt.getName(), mt.fields());
		for (Field f : fieldsToRemove) {
			mt.removeDeclaredField(f);
			String documentation = StringUtils.trimToEmpty(mt.documentation());
			documentation += " NOTE: Removed field " + f;
			mt.updateDocumentation(documentation);
		}

		List<OneOf> oneOfsToRemove = new ArrayList<>();

		for (OneOf oneOf : mt.oneOfs()) {
			List<Field> oneOfFieldsToRemove = getFieldsToRemove(file.packageName(), mt.getName(), oneOf.fields());
			for (Field f : oneOfFieldsToRemove) {
				oneOf.fields().remove(f);

				String documentation = StringUtils.trimToEmpty(mt.documentation());
				documentation += " NOTE: Removed field " + f;
				oneOf.updateDocumentation(documentation);
			}

			if (oneOf.fields().size() == 0) {
				// remove oneof
				oneOfsToRemove.add(oneOf);
			}
		}

		// Remove empty oneOfs
		for (OneOf oneOfToRemove : oneOfsToRemove) {

			mt.removeOneOf(oneOfToRemove);
			String documentation = StringUtils.trimToEmpty(mt.documentation());
			documentation += " NOTE: Removed empty oneOf " + oneOfToRemove;
			mt.updateDocumentation(documentation);
		}
	}

	private List<Field> getFieldsToRemove(String packageName, String messageName, List<Field> fields) {
		return fields.stream().filter(field -> typeAndFieldNameMapper.ignoreOutputField(packageName, messageName, field.name())).collect(Collectors.toList());
	}
}
