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
import java.util.Objects;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.ProtoFile;

import no.entur.schema2proto.generateproto.Schema2ProtoConfiguration;

public class AddConfigurationSpecifiedImports implements Processor {
	private static final String VALIDATION_PROTO_IMPORT = "validate/validate.proto";

	private Schema2ProtoConfiguration configuration;

	public AddConfigurationSpecifiedImports(Schema2ProtoConfiguration configuration) {
		this.configuration = configuration;
	}

	@Override
	public void process(Map<String, ProtoFile> packageToProtoFileMap) {
		List<String> imports = configuration.customImports.stream().map(this::getPackageFromPathName).collect(Collectors.toList());
		packageToProtoFileMap.values().forEach(file -> {
			imports.forEach(i -> {
				if (messageTypes(file.types()).anyMatch(type -> isCustomImportInUseInNestedTypes(i, type))) {
					file.imports().add(i);
				}
			});
			if (configuration.includeValidationRules) {
				file.imports().add(VALIDATION_PROTO_IMPORT);
			}
		});

	}

	private boolean isCustomImportInUseInNestedTypes(String importPackage, MessageType type) {
		if (type.fieldsAndOneOfFields().stream().anyMatch(f -> Objects.equals(f.getElementType(), importPackage))) {
			return true;
		}
		return messageTypes(type.nestedTypes()).anyMatch(t -> isCustomImportInUseInNestedTypes(importPackage, t));

	}

	// converts e.g. google/protobuf/timestamp.proto => google.protobuf.timestamp
	@NotNull
	private String getPackageFromPathName(String pathName) {
		return pathName.replace(".proto", "").replace('/', '.');
	}
}