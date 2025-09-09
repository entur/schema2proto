/*-
 * #%L
 * schema2proto-lib
 * %%
 * Copyright (C) 2019 - 2020 Entur
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
package no.entur.schema2proto.generateproto;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import no.entur.schema2proto.AbstractMappingTest;

public class GenerateValidationRulesTest extends AbstractMappingTest {

	@Test
	@Disabled
	public void messageIsRequired() throws IOException {
		generateProtobuf("test-message-required.xsd", validationOptions());
		String generated = IOUtils.toString(Files.newInputStream(Paths.get("target/generated-proto/default/default.proto")), Charset.defaultCharset());
		Assertions.assertEquals(generated,
				"// default.proto at 0:0\n" + "syntax = \"proto3\";\n" + "package default;\n" + "\n" + "import \"buf/validate/validate.proto\";\n" + "\n"
						+ "message TestRangeInt {\n" + "  int32 value = 1 [\n" + "    (buf.validate.field).required = true\n" + "  ];\n" + "}\n");
	}

	@Test
	@Disabled
	public void repeatedFieldWithRange() throws IOException {
		generateProtobuf("test-min-max-occurs-range.xsd", validationOptions());
		String generated = IOUtils.toString(Files.newInputStream(Paths.get("target/generated-proto/default/default.proto")), Charset.defaultCharset());
		Assertions.assertEquals(generated,
				"// default.proto at 0:0\n" + "syntax = \"proto3\";\n" + "package default;\n" + "\n" + "import \"buf/validate/validate.proto\";\n" + "\n"
						+ "message TestRangeDecimal {\n" + "  repeated double value = 1 [\n" + "    (buf.validate.field).repeated = {\n"
						+ "      min_items: 1,\n" + "      max_items: 7\n" + "    }\n" + "  ];\n" + "}\n");
	}

	@Test
	@Disabled
	public void repeatedFieldUnbounded() throws IOException {
		generateProtobuf("test-min-max-occurs-unbounded.xsd", validationOptions());
		String generated = IOUtils.toString(Files.newInputStream(Paths.get("target/generated-proto/default/default.proto")), Charset.defaultCharset());
		Assertions.assertEquals(generated,
				"// default.proto at 0:0\n" + "syntax = \"proto3\";\n" + "package default;\n" + "\n" + "import \"buf/validate/validate.proto\";\n" + "\n"
						+ "message TestRangeDecimal {\n" + "  repeated double value = 1 [\n" + "    (buf.validate.field).repeated = {\n"
						+ "      min_items: 1\n" + "    }\n" + "  ];\n" + "}\n");
	}

	@Test
	public void attributeOptionalPositiveInteger_shouldAllow0() throws IOException {
		generateProtobuf("test-attribute-positiveInteger.xsd", validationOptions());
		String generated = IOUtils.toString(Files.newInputStream(Paths.get("target/generated-proto/default/default.proto")), Charset.defaultCharset());
		Assertions.assertEquals("// default.proto at 0:0\n" + "syntax = \"proto3\";\n" + "package default;\n" + "\n"
				+ "import \"buf/validate/validate.proto\";\n" + "\n" + "message TestOptionalPositiveIntegerAttribute {\n" + "  uint32 optional_attribute = 1;\n"
				+ "  uint32 required_attribute = 2 [\n" + "    (buf.validate.field).uint32.gt = 0\n" + "  ];\n" + "}\n", generated);
	}

	private Schema2ProtoConfiguration validationOptions() {
		Schema2ProtoConfiguration configuration = new Schema2ProtoConfiguration();
		configuration.forceProtoPackage = "default";
		configuration.includeValidationRules = true;
		configuration.customImportLocations = Arrays.asList("src/test/resources", "target/proto_deps");
		return configuration;
	}
}
