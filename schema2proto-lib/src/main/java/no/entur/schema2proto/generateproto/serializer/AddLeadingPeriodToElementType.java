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

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.Type;

/*
 * Adds leading '.' to field.elementType when needed. Ref.: https://developers.google.com/protocol-buffers/docs/proto3#packages-and-name-resolution
 */
public class AddLeadingPeriodToElementType implements Processor {

	@Override
	public void process(Map<String, ProtoFile> packageToProtoFileMap) {
		for (ProtoFile file : packageToProtoFileMap.values()) {
			for (Type type : file.types()) {
				if (type instanceof MessageType) {
					MessageType mt = (MessageType) type;
					// TODO must this be done for nested types as well or handled differently?
					for (Field field : mt.fieldsAndOneOfFields()) {

						String fieldElementType = StringUtils.trimToNull(field.getElementType());
						if (fieldElementType != null) {
							if (fieldElementType.contains(".")) {
								for (String pkg : packageToProtoFileMap.keySet()) {
									if (!fieldElementType.equals(pkg)) {
										String rootFieldElementType = fieldElementType.split("\\.")[0];
										if (pkg.contains("." + rootFieldElementType + ".")) {
											// elementType should only be prepended when root-package of elementType matches a non-root package
											field.updateElementType("." + fieldElementType);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
}
