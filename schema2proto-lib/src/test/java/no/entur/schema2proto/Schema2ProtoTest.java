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

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class Schema2ProtoTest {

	@Test
	public void compareAtomProtobuf() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/atom.proto", generateProtobuf("atom"));
	}

	@Test
	public void compareRecipeProtobuf() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/recipe.proto", generateProtobuf("recipe"));
	}

	@Test
	public void compareShiporderProtobuf() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/shiporder.proto", generateProtobuf("shiporder"));
	}

	@Test
	public void compareTestChoiceProtobuf() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/test-choice.proto", generateProtobuf("test-choice"));
	}

	@Test
	public void compareTestDatatypesProtobuf() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/test-datatypes.proto", generateProtobuf("test-datatypes"));
	}

	@Test
	public void compareTestDatatypesStringDatesProtobuf() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/test-datatypes-string-dates.proto",
				generateProtobuf("test-datatypes-string-dates", "^date$:string,^dateTime$:string", null, "default", false));
	}

	@Test
	public void compareTestExtensionProtobuf() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/test-extension.proto", generateProtobuf("test-extension"));
	}

	@Test
	public void compareTestExtensionAttributesProtobuf() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/test-extension-attributes.proto", generateProtobuf("test-extension-attributes"));
	}

	@Test
	public void compareTestRangeProtobuf() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/test-range.proto", generateProtobuf("test-range"));
	}

	// @Test
	public void compareXmlRecipemlProtobuf() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/xml-recipeml.proto", generateProtobuf("xml-recipeml"));
	}

	// @Test
	public void compareRecipemlProtobuf() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/recipeml.proto", generateProtobuf("recipeml"));
	}

	@Test
	public void fieldAndMessageRenaming() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/complexTypeRenaming.proto", generateProtobuf("complexTypeRenaming",
				"^ElementListOriginalNameType$:ElementListNewNameType,^ElementInListOfComplexTypeOriginalName$:ElementInListOfComplexTypeNewName,^ComplexTypeOriginalName$:ComplexTypeNewName,^ElementInListOriginalName$:ElementInListNewName",
				"^ElementListOriginalNameType$:ElementListNewNameType,^ElementInListOfComplexTypeOriginalName$:ElementInListOfComplexTypeNewName,^ComplexTypeOriginalName$:ComplexTypeNewName,^ElementInListOriginalName$:ElementInListNewName",
				"org.myrecipies", false));
	}

}
