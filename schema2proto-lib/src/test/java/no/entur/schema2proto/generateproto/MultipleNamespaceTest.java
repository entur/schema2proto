
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
package no.entur.schema2proto.generateproto;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import no.entur.schema2proto.AbstractMappingTest;

public class MultipleNamespaceTest extends AbstractMappingTest {

	private File expectedRootFolder = new File("src/test/resources/expectedproto/multinamespace");
	private File generatedRootFolder = new File("target/generated-proto/multinamespace");

	@Test
	public void shouldCreateANamespacedProtobufPersonFile() throws IOException {
		Schema2Proto.main(new String[] { "--outputDirectory=target/generated-proto/multinamespace/", "src/test/resources/xsd/multinamespace/ns-person.xsd" });

		compareExpectedAndGenerated(expectedRootFolder, "com/schemas/domain/person/com_schemas_domain_person.proto", generatedRootFolder,
				"com/schemas/domain/person/com_schemas_domain_person.proto");
		compareExpectedAndGenerated(expectedRootFolder, "com/schemas/domain/common/com_schemas_domain_common.proto", generatedRootFolder,
				"com/schemas/domain/common/com_schemas_domain_common.proto");
		compareExpectedAndGenerated(expectedRootFolder, "com/schemas/domain/address/com_schemas_domain_address.proto", generatedRootFolder,
				"com/schemas/domain/address/com_schemas_domain_address.proto");
	}

	@Test
	public void shouldReferenceListItemTypeInItsOwnNamespace() throws IOException {
		// Generation fails if the files do not link, remove output from earlier runs so it is not compared instead
		FileUtils.deleteDirectory(new File(generatedRootFolder, "com/schemas/domain/listmain"));
		FileUtils.deleteDirectory(new File(generatedRootFolder, "com/schemas/domain/listenums"));
		Schema2Proto
				.main(new String[] { "--outputDirectory=target/generated-proto/multinamespace/", "src/test/resources/xsd/multinamespace/ns-list-main.xsd" });

		compareExpectedAndGenerated(expectedRootFolder, "com/schemas/domain/listmain/com_schemas_domain_listmain.proto", generatedRootFolder,
				"com/schemas/domain/listmain/com_schemas_domain_listmain.proto");
		compareExpectedAndGenerated(expectedRootFolder, "com/schemas/domain/listenums/com_schemas_domain_listenums.proto", generatedRootFolder,
				"com/schemas/domain/listenums/com_schemas_domain_listenums.proto");
	}

}
