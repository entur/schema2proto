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
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MultipleNamespaceTest {

	@BeforeAll
	public static void generateProtobufForTests() {
		try {
			new File("target/generated-proto").mkdirs();
			Schema2Proto.main(new String[] { "--splitBySchema=true", "--directory=target/generated-proto/", "--package=schemas.com.domain.common",
					"src/test/resources/xsd/ns-person.xsd" });
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void shouldCreateANamespacedProtobufPersonFile() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/schemas_com_domain_person.proto",
				"target/generated-proto/schemas_com_domain_person.proto");
	}

	@Test
	public void shouldCreateANamespacedProtobufCommonFile() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/schemas_com_domain_common.proto",
				"target/generated-proto/schemas_com_domain_common.proto");
	}

	@Test
	public void shouldCreateANamespacedProtobufAddressFile() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/schemas_com_domain_address.proto",
				"target/generated-proto/schemas_com_domain_address.proto");
	}
}
