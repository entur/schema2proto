package no.entur.schema2proto.compatibility;

/*-
 * #%L
 * schema2proto-lib
 * %%
 * Copyright (C) 2019 - 2020 Entur
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

import java.io.IOException;

import org.junit.jupiter.api.Test;

public class ProtolockBackwardsCompatibilityCheckerFieldTest extends AbstractBackwardsCompatTest {

	@Test
	public void testAddedField() throws IOException {
		verify("newfield", true, "default/default.proto");
	}

	@Test
	public void testRemovedField() throws IOException {
		verify("removedfield", false, "default/default.proto");
	}

	@Test
	public void testAddFieldExistingReservation() throws IOException {
		verify("existingreservation", false, "default/default.proto");
	}

	@Test
	public void testInjectField() throws IOException {
		verify("injectedfield", true, "default/default.proto");
	}

	@Test
	public void testChangedFieldTag() throws IOException {
		verify("changedfieldtag", true, "default/default.proto");
	}

	@Test
	public void testChangedFieldName() throws IOException {
		verify("changedfieldname", false, "default/default.proto");
	}

	@Test
	public void testNewAndRemovedField() throws IOException {
		verify("newandremovedfield", false, "default/default.proto");
	}

	@Test
	public void testNestedMessageWithOneOf() throws IOException {
		verify("nestedmessagewithoneof", true, "default/default.proto");
	}

	@Test
	public void testNestedMessageWithOneOfReorganizedFields() throws IOException {
		verify("nestedmessagewithoneof_reorganizedfields", true, "default/default.proto");
	}

	@Test
	public void testBugIgnoredReservation() throws IOException {
		verify("bug_ignoredreservation", false, "uk/org/netex/www/netex/uk_org_netex_www_netex.proto");
	}

}
