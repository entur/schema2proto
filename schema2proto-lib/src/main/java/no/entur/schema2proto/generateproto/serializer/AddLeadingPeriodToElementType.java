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
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;

import com.squareup.wire.schema.ProtoFile;

/*
 * Adds leading '.' to field.elementType when needed. Ref.: https://developers.google.com/protocol-buffers/docs/proto3#packages-and-name-resolution
 */
public class AddLeadingPeriodToElementType implements Processor {

	@Override
	public void process(Map<String, ProtoFile> packageToProtoFileMap) {
		packageToProtoFileMap.values()
				.stream()
				.flatMap(f -> messageTypes(f.types()))
				.flatMap(mt -> mt.fieldsAndOneOfFields().stream())
				.filter(field -> Objects.nonNull(StringUtils.trimToNull(field.getElementType())) && field.getElementType().contains(".")
						&& !packageToProtoFileMap.containsKey(field.getElementType())
						&& packageToProtoFileMap.containsKey("." + field.getElementType().split("\\.")[0] + "."))
				.forEach(field -> field.updateElementType("." + field.getElementType()));
	}
}
