package no.entur.schema2proto;

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

import static no.entur.schema2proto.TestHelper.compareExpectedAndGenerated;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MultipleNamespaceTest {

	@BeforeAll
	public static void generateProtobufForTests() {
		Schema2Proto.main(new String[] { "--outputDirectory=target/generated-proto/multinamespace/", "src/test/resources/xsd/multinamespace/ns-person.xsd" });
	}

	@Test
	public void shouldCreateANamespacedProtobufPersonFile() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/multinamespace/com_schemas_domain_person.proto",
				"target/generated-proto/multinamespace/com_schemas_domain_person.proto");
	}

	@Test
	public void shouldCreateANamespacedProtobufCommonFile() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/multinamespace/com_schemas_domain_common.proto",
				"target/generated-proto/multinamespace/com_schemas_domain_common.proto");
	}

	@Test
	public void shouldCreateANamespacedProtobufAddressFile() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/multinamespace/com_schemas_domain_address.proto",
				"target/generated-proto/multinamespace/com_schemas_domain_address.proto");
	}
}
