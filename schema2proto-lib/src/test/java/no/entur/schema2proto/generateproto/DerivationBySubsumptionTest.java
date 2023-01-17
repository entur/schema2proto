
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

import org.junit.jupiter.api.Test;

import no.entur.schema2proto.AbstractMappingTest;

public class DerivationBySubsumptionTest extends AbstractMappingTest {

	private File expectedRootFolder = new File("src/test/resources/expectedproto/nato");
	private File generatedRootFolder = new File("target/generated-proto/nato");

	@Test
	public void testDerivationBySubsumption() {
		Schema2Proto.main(new String[] { "--outputDirectory=target/generated-proto/nato/", "--derivationBySubsumption=true",
				"src/test/resources/xsd/nato/Locations.xsd" });

		compareExpectedAndGenerated(expectedRootFolder, "vision/combat/xsds/v2017/v05/v12/iescore/vision_combat_xsds_v2017_v05_v12_iescore.proto",
				generatedRootFolder, "vision/combat/xsds/v2017/v05/v12/iescore/vision_combat_xsds_v2017_v05_v12_iescore.proto");
		compareExpectedAndGenerated(expectedRootFolder, "vision/combat/xsds/v2017/v05/v12/locations/vision_combat_xsds_v2017_v05_v12_locations.proto",
				generatedRootFolder, "vision/combat/xsds/v2017/v05/v12/locations/vision_combat_xsds_v2017_v05_v12_locations.proto");
	}

}
