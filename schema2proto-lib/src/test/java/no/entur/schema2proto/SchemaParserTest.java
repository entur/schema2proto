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

public class SchemaParserTest {

	@Test
	public void testNestedTopLevelElement() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/basic/nestedtoplevelelement.proto", generateProtobuf("basic/nestedtoplevelelement"));
	}

	@Test
	public void testBasicTopLevelEnum() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/basic/toplevelenum.proto", generateProtobuf("basic/toplevelenum"));
	}

	@Test
	public void testBasicTopLevelComplexType() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/basic/toplevelcomplextype.proto", generateProtobuf("basic/toplevelcomplextype"));
	}

	@Test
	public void testBasicTopLevelSimpleType() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/basic/toplevelsimpletype.proto", generateProtobuf("basic/toplevelsimpletype"));
	}

	@Test
	public void testBasicExtensionBase() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/basic/extensionbase.proto", generateProtobuf("basic/extensionbase"));
	}

	@Test
	public void testBasicExtensionBaseComposition() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/basic/extensionbase_composition.proto",
				generateProtobuf("basic/extensionbase", null, null, null, true));
	}

	@Test
	public void testBasicExtensionBase2() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/basic/extensionbase2.proto", generateProtobuf("basic/extensionbase2"));
	}

	@Test
	public void testExtensionBaseDuplicateElements() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/basic/extensionbaseduplicateelements.proto",
				generateProtobuf("basic/extensionbaseduplicateelements"));
	}

	@Test
	public void testAttributeGroups() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/basic/attributegroups.proto", generateProtobuf("basic/attributegroups"));
	}

	@Test
	public void testElementRef() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/basic/elementref.proto", generateProtobuf("basic/elementref"));
	}

	@Test
	public void testRestrictionBase() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/basic/restrictionbaseduplicateelements.proto",
				generateProtobuf("basic/restrictionbaseduplicateelements"));
	}

}
