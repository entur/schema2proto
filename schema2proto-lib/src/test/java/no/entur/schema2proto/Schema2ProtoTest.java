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
import static no.entur.schema2proto.TestHelper.generateProtobuf;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Test;

public class Schema2ProtoTest {

	private File expectedRootFolder = new File("src/test/resources/expectedproto/legacy");
	private File generatedRootFolder = new File(".");

	@Test
	public void compareAtomProtobuf() throws IOException {
		compareExpectedAndGenerated(expectedRootFolder, "default/atom.proto", generatedRootFolder, generateProtobuf("atom.xsd", "default", "default.proto"));
	}

	@Test
	public void compareRecipeProtobuf() throws IOException {
		compareExpectedAndGenerated(expectedRootFolder, "default/recipe.proto", generatedRootFolder,
				generateProtobuf("recipe.xsd", "default", "default.proto"));
	}

	@Test
	public void compareShiporderProtobuf() throws IOException {
		compareExpectedAndGenerated(expectedRootFolder, "default/shiporder.proto", generatedRootFolder,
				generateProtobuf("shiporder.xsd", "default", "default.proto"));
	}

	@Test
	public void compareTestChoiceProtobuf() throws IOException {
		compareExpectedAndGenerated(expectedRootFolder, "default/test-choice.proto", generatedRootFolder,
				generateProtobuf("test-choice.xsd", "default", "default.proto"));
	}

	@Test
	public void compareTestDatatypesProtobuf() throws IOException {
		compareExpectedAndGenerated(expectedRootFolder, "default/test-datatypes.proto", generatedRootFolder,
				generateProtobuf("test-datatypes.xsd", "default", "default.proto"));
	}

	@Test
	public void compareTestExtensionProtobuf() throws IOException {
		compareExpectedAndGenerated(expectedRootFolder, "default/test-extension.proto", generatedRootFolder,
				generateProtobuf("test-extension.xsd", "default", "default.proto"));
	}

	@Test
	public void compareTestExtensionAttributesProtobuf() throws IOException {
		compareExpectedAndGenerated(expectedRootFolder, "default/test-extension-attributes.proto", generatedRootFolder,
				generateProtobuf("test-extension-attributes.xsd", "default", "default.proto"));
	}

	@Test
	public void compareTestRangeProtobuf() throws IOException {
		compareExpectedAndGenerated(expectedRootFolder, "default/test-range.proto", generatedRootFolder,
				generateProtobuf("test-range.xsd", "default", "default.proto"));
	}

	// @Test
	public void compareXmlRecipemlProtobuf() throws IOException {
		compareExpectedAndGenerated(expectedRootFolder, "default/xml-recipeml.proto", generatedRootFolder,
				generateProtobuf("xml-recipeml.xsd", "default", "default.proto"));
	}

	// @Test
	public void compareRecipemlProtobuf() throws IOException {
		compareExpectedAndGenerated(expectedRootFolder, "default/recipeml.proto", generatedRootFolder,
				generateProtobuf("recipeml.xsd", "default", "default.proto"));
	}

	@Test
	public void fieldAndMessageRenaming() throws IOException {
		compareExpectedAndGenerated(expectedRootFolder, "default/complexTypeRenaming.proto", generatedRootFolder, generateProtobuf("complexTypeRenaming.xsd",
				"^ElementListOriginalNameType$:ElementListNewNameType,^ElementInListOfComplexTypeOriginalName$:ElementInListOfComplexTypeNewName,^ComplexTypeOriginalName$:ComplexTypeNewName,^ElementInListOriginalName$:ElementInListNewName",
				"^ElementListOriginalNameType$:ElementListNewNameType,^ElementInListOfComplexTypeOriginalName$:ElementInListOfComplexTypeNewName,^ComplexTypeOriginalName$:ComplexTypeNewName,^ElementInListOriginalName$:ElementInListNewName",
				"org.myrecipies", false, "org/myrecipies", "org_myrecipies.proto"));
	}

}
