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

public class ProtolockBackwardsCompatibilityCheckerEnumTest extends AbstractBackwardsCompatTest {

	@Test
	public void testAddedEnumConstant() throws IOException {
		verify("newenumconstant", true, "default/default.proto");
	}

	@Test
	public void testRemovedEnumConstant() throws IOException {
		verify("removedenumconstant", false, "default/default.proto");
	}

	@Test
	public void testAddEnumConstantExistingReservation() throws IOException {
		verify("existingreservationenumconstant", false, "default/default.proto");
	}

	@Test
	public void testInjectEnumConstant() throws IOException {
		verify("injectenumconstant", true, "default/default.proto");
	}

	@Test
	public void testChangedEnumConstantTag() throws IOException {
		verify("changedenumconstanttag", true, "default/default.proto");
	}

	@Test
	public void testChangedEnumConstantName() throws IOException {
		verify("changedenumconstantname", false, "default/default.proto");
	}

	@Test
	public void testNewAndRemoveEnumConstant() throws IOException {
		verify("newandremovedenumconstant", false, "default/default.proto");
	}

	@Test
	public void testNestedMessageWithEnum() throws IOException {
		verify("nestedmessagewithenum", false, "default/default.proto");
	}

}
