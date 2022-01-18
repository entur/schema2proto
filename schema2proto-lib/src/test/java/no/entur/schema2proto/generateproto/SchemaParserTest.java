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

import org.junit.jupiter.api.Test;

import no.entur.schema2proto.AbstractMappingTest;

@SuppressWarnings("java:S2699")
public class SchemaParserTest extends AbstractMappingTest {

	private File expectedRootFolder = new File("src/test/resources/expectedproto/basic");

	@Test
	public void testAnyTypeElement() throws IOException {
		generateProtobufNoOptions("basic/anyType.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/anytype.proto", generatedRootFolder, "default/default.proto");

	}

	@Test
	public void testNestedTopLevelElement() throws IOException {
		generateProtobufNoOptions("basic/nestedtoplevelelement.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/nestedtoplevelelement.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void testBasicTopLevelEnum() throws IOException {
		generateProtobufNoOptions("basic/toplevelenum.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/toplevelenum.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void testEnumWithSpecialChars() throws IOException {
		generateProtobufNoOptions("basic/enumspecialchars.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/enumspecialchars.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void testAvoidDuplicateChoiceWrapper() throws IOException {
		generateProtobufNoOptions("basic/duplicatechoicewrapper.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/duplicatechoicewrapper.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void testBasicTopLevelComplexType() throws IOException {
		generateProtobufNoOptions("basic/toplevelcomplextype.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/toplevelcomplextype.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void testBasicTopLevelSimpleType() throws IOException {
		generateProtobufNoOptions("basic/toplevelsimpletype.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/toplevelsimpletype.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void testBasicExtensionBase() throws IOException {
		generateProtobufNoOptions("basic/extensionbase.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/extensionbase.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void testEmptyGroup() throws IOException {
		generateProtobufNoOptions("basic/emptygroup.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/emptygroup.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void testBasicExtensionBaseComposition() throws IOException {
		Schema2ProtoConfiguration configuration = new Schema2ProtoConfiguration();
		configuration.inheritanceToComposition = true;
		generateProtobuf("basic/extensionbase.xsd", configuration);
		compareExpectedAndGenerated(expectedRootFolder, "default/extensionbase_composition.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void testBasicInheritanceToComposition() throws IOException {
		Schema2ProtoConfiguration configuration = new Schema2ProtoConfiguration();
		configuration.inheritanceToComposition = true;
		generateProtobuf("basic/inheritancetocomposition.xsd", configuration);
		compareExpectedAndGenerated(expectedRootFolder, "default/inheritancetocomposition.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void testBasicExtensionBase2() throws IOException {
		generateProtobufNoOptions("basic/extensionbase2.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/extensionbase2.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void testExtensionBaseDuplicateElements() throws IOException {
		generateProtobufNoOptions("basic/extensionbaseduplicateelements.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/extensionbaseduplicateelements.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void testElementExtensions() throws IOException {
		generateProtobufNoOptions("basic/extendedelement.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/extendedelement.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void testDifferentInnerClassesWithSameName() throws IOException {
		generateProtobufNoOptions("basic/differentinnerclasseswithsamename.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/differentinnerclasseswithsamename.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void testAttributeGroups() throws IOException {
		generateProtobufNoOptions("basic/attributegroups.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/attributegroups.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void testElementRef() throws IOException {
		generateProtobufNoOptions("basic/elementref.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/elementref.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void testSkipEmptyTypeInheritance() throws IOException {
		Schema2ProtoConfiguration configuration = new Schema2ProtoConfiguration();
		configuration.skipEmptyTypeInheritance = true;
		generateProtobufNoTypeOrNameMappings("basic/skipemptytypeinheritance.xsd", configuration);
		compareExpectedAndGenerated(expectedRootFolder, "default/skipemptytypeinheritance.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void testUnboundedChoice() throws IOException {
		generateProtobufNoOptions("basic/unboundedchoices.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/unboundedchoices.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void testUnboundedSequence() throws IOException {
		generateProtobufNoOptions("basic/unboundedsequences.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/unboundedsequences.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void testUnboundedNestedSequence() throws IOException {
		generateProtobufNoOptions("basic/unboundednestedsequences.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/unboundednestedsequences.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void testList() throws IOException {
		generateProtobufNoOptions("basic/list.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/list.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void testUnion() throws IOException {
		generateProtobufNoOptions("basic/union.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/union.proto", generatedRootFolder, "default/default.proto");
	}

	// @Test
	public void testValidationRules() throws IOException {
		generateProtobufNoOptions("basic/validationrules.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/validationrules.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void testRestrictionBase() throws IOException {
		generateProtobufNoOptions("basic/restrictionbaseduplicateelements.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/restrictionbaseduplicateelements.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void testLocalToGlobalConversion() throws IOException {
		generateProtobufNoOptions("basic/localtoglobal.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/localtoglobal.proto", generatedRootFolder, "default/default.proto");
	}

	@Test
	public void testPackedOptionOnRepeatedScalarTypes() throws IOException {
		generateProtobufNoOptions("basic/packed.xsd");
		compareExpectedAndGenerated(expectedRootFolder, "default/packed.proto", generatedRootFolder, "default/default.proto");
	}

	// @Test
	public void testIncludeXsdOptions() throws IOException {
		Schema2ProtoConfiguration configuration = new Schema2ProtoConfiguration();
		configuration.includeXsdOptions = true;
		generateProtobufNoTypeOrNameMappings("basic/xsdoption.xsd", configuration);
		compareExpectedAndGenerated(expectedRootFolder, "default/xsdoption.proto", generatedRootFolder, "default/default.proto");
	}

}
