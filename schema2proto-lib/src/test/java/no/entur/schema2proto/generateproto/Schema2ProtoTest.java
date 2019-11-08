package no.entur.schema2proto.generateproto;

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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import no.entur.schema2proto.AbstractMappingTest;

public class Schema2ProtoTest extends AbstractMappingTest {

	private File expectedRootFolder = new File("src/test/resources/expectedproto/legacy");

	@Test
	public void compareAtomProtobuf() throws IOException {

		generateProtobufNoOptions("atom.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/atom.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void compareRecipeProtobuf() throws IOException {
		generateProtobufNoOptions("recipe.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/recipe.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void compareShiporderProtobuf() throws IOException {
		generateProtobufNoOptions("shiporder.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/shiporder.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void compareTestChoiceProtobuf() throws IOException {
		generateProtobufNoOptions("test-choice.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/test-choice.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void compareTestDatatypesProtobuf() throws IOException {
		generateProtobufNoOptions("test-datatypes.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/test-datatypes.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void compareTestExtensionProtobuf() throws IOException {
		generateProtobufNoOptions("test-extension.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/test-extension.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void compareTestExtensionAttributesProtobuf() throws IOException {
		generateProtobufNoOptions("test-extension-attributes.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/test-extension-attributes.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void compareTestRangeProtobuf() throws IOException {
		generateProtobufNoOptions("test-range.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/test-range.proto", generatedRootFolder, "default/default.proto");
	}

	// @Test
	public void compareXmlRecipemlProtobuf() throws IOException {
		generateProtobufNoOptions("xml-recipeml.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/xml-recipeml.proto", generatedRootFolder, "default/default.proto");
	}

	// @Test
	public void compareRecipemlProtobuf() throws IOException {
		generateProtobufNoOptions("recipeml.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/recipeml.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void fieldAndMessageRenaming() throws IOException {
		Map<String, Object> options = new HashMap<>();
		generateProtobuf("complexTypeRenaming.xsd",
				"^ElementListOriginalNameType$:ElementListNewNameType,^ElementInListOfComplexTypeOriginalName$:ElementInListOfComplexTypeNewName,^ComplexTypeOriginalName$:ComplexTypeNewName,^ElementInListOriginalName$:ElementInListNewName",
				"^ElementListOriginalNameType$:ElementListNewNameType,^ElementInListOfComplexTypeOriginalName$:ElementInListOfComplexTypeNewName,^ComplexTypeOriginalName$:ComplexTypeNewName,^ElementInListOriginalName$:ElementInListNewName",
				"org.myrecipies", false, options);
		compareExpectedAndGenerated(expectedRootFolder, "default/complexTypeRenaming.proto", generatedRootFolder, "org/myrecipies/org_myrecipies.proto");
	}

}
