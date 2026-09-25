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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

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

	/**
	 * A field whose declared number is already taken in proto.lock by a different field, and which proto.lock does not know by name, gets a brand new number.
	 * That silently breaks wire compatibility with the input, so it must be reported.
	 */
	@Test
	public void testInjectedFieldIsReportedAsRenumbered() throws IOException {
		List<FieldConflictChecker.FieldRenumbering> renumberings = verify("injectedfield", true, "default/default.proto").getFieldRenumberings();

		assertEquals(1, renumberings.size(), "expected exactly one renumbering, got " + renumberings);
		FieldConflictChecker.FieldRenumbering renumbering = renumberings.get(0);
		assertEquals("ElementList", renumbering.message);
		assertEquals("injected_field1", renumbering.field);
		assertEquals(2, renumbering.declaredTag);
		assertEquals(8, renumbering.assignedTag);
		assertTrue(renumbering.reason.contains("'second'"), "reason should name the field holding the number: " + renumbering.reason);
	}

	/**
	 * A genuinely new field on a number nobody else claims keeps that number, and is not a renumbering. Restoring a field to the number proto.lock already
	 * records for that name is likewise the lock doing its job, and must not be reported either.
	 */
	@Test
	public void testNewFieldOnFreeNumberIsNotReportedAsRenumbered() throws IOException {
		assertTrue(verify("newfield", true, "default/default.proto").getFieldRenumberings().isEmpty());
	}

	/**
	 * Renumberings are reported for nested messages too, not just top level ones. In this fixture "fourth" is unknown to proto.lock and is declared on number
	 * 2, which proto.lock gives to "second", so it is pushed aside in both ElementList and its nested SubElement.
	 */
	@Test
	public void testRenumberingIsReportedForNestedMessages() throws IOException {
		List<FieldConflictChecker.FieldRenumbering> renumberings = verify("existingreservation", false, "default/default.proto").getFieldRenumberings();

		assertEquals(2, renumberings.size(), "expected one renumbering per message, got " + renumberings);
		assertTrue(renumberings.stream().anyMatch(r -> "ElementList".equals(r.message) && "fourth".equals(r.field) && r.assignedTag == 4),
				"missing ElementList#fourth: " + renumberings);
		assertTrue(renumberings.stream().anyMatch(r -> "SubElement".equals(r.message) && "fourth".equals(r.field) && r.assignedTag == 4),
				"missing SubElement#fourth: " + renumberings);
	}

}
