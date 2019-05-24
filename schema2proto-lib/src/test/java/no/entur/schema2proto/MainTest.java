package no.entur.schema2proto;

import static no.entur.schema2proto.TestHelper.compareExpectedAndGenerated;
import static no.entur.schema2proto.TestHelper.generateProtobuf;

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class MainTest {

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
				generateProtobuf("test-datatypes-string-dates", "^date$:string,^dateTime$:string", null, "default"));
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
	public void compareTestOptionalProtobuf() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/test-optional.proto", generateProtobuf("test-optional"));
	}

	@Test
	public void compareTestRangeProtobuf() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/test-range.proto", generateProtobuf("test-range"));
	}

	@Test
	public void compareXmlRecipemlProtobuf() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/xml-recipeml.proto", generateProtobuf("xml-recipeml"));
	}

	@Test
	public void compareRecipemlProtobuf() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/recipeml.proto", generateProtobuf("recipeml"));
	}

	@Test
	public void fieldAndMessageRenaming() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/complexTypeRenaming.proto", generateProtobuf("complexTypeRenaming",
				"^ElementListOriginalName$:ElementListNewName,^ElementInListOfComplexTypeOriginalName$:ElementInListOfComplexTypeNewName,^ComplexTypeOriginalName$:ComplexTypeNewName,^ElementInListOriginalName$:ElementInListNewName",
				"^ElementListOriginalName$:ElementListNewName,^ElementInListOfComplexTypeOriginalName$:ElementInListOfComplexTypeNewName,^ComplexTypeOriginalName$:ComplexTypeNewName,^ElementInListOriginalName$:ElementInListNewName",
				"org.myrecipies"));
	}

}
