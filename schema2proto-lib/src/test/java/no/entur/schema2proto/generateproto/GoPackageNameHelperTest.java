package no.entur.schema2proto.generateproto;

/*-
 * #%L
 * schema2proto-lib
 * %%
 * Copyright (C) 2019 - 2021 Entur
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

import org.junit.jupiter.api.Test;

public class GoPackageNameHelperTest {

	@Test
	public void whenLastPartInPackageNameIsNumeric_thenUseTwoLastPartsCombinedAsGoPackageName() {
		assertEquals("net/opengis/www/gml32", GoPackageNameHelper.packageNameToGoPackageName(null, "net.opengis.www.gml._3_2"));
		assertEquals("test.org/proto/net/opengis/www/gml32", GoPackageNameHelper.packageNameToGoPackageName("test.org/proto/", "net.opengis.www.gml._3_2"));
	}

	@Test
	public void whenLastPartInPackageNameIsNotNumeric_thenUsePartAsGoPackageName() {
		assertEquals("uk/org/netex/www/netex", GoPackageNameHelper.packageNameToGoPackageName(null, "uk.org.netex.www.netex"));
		assertEquals("test.org/proto/uk/org/netex/www/netex", GoPackageNameHelper.packageNameToGoPackageName("test.org/proto/", "uk.org.netex.www.netex"));
	}
}
