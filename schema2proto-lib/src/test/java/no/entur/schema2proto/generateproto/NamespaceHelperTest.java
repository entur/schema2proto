package no.entur.schema2proto.generateproto;

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

import org.junit.jupiter.api.Test;

import de.slub.urn.URNSyntaxError;

public class NamespaceHelperTest {
	@Test
	void testConvertUrn() throws URNSyntaxError {
		assertEquals("com.test", NamespaceHelper.convertAsUrn("urn:com:test"));
		assertEquals("nbn.de.v101.v1_2019072802401757702913", NamespaceHelper.convertAsUrn("urn:nbn:de:101:1-2019072802401757702913"));
		assertEquals("oasis.names.specification.docbook.dtd.xml.v4_1_2", NamespaceHelper.convertAsUrn("urn:oasis:names:specification:docbook:dtd:xml:4.1.2"));
		assertEquals("com.mycompany.myproject.mymessage.v20200901.v001", NamespaceHelper.convertAsUrn("urn:com:mycompany:myproject:mymessage:20200901:001"));
	}

	@Test
	void testConvertNamespace() throws URNSyntaxError {
		assertEquals("com.test", NamespaceHelper.xmlNamespaceToProtoPackage("urn:com:test", null));
		assertEquals("com.test", NamespaceHelper.xmlNamespaceToProtoPackage("http://test.com", null));
		assertEquals("net.opengis.www.gml.v3_2", NamespaceHelper.xmlNamespaceToProtoPackage("http://www.opengis.net/gml/3.2", null));
	}

}
