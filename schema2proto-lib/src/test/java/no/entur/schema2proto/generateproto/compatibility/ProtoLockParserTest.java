package no.entur.schema2proto.generateproto.compatibility;

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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;

import org.junit.jupiter.api.Test;

import no.entur.schema2proto.generateproto.compatibility.protolock.ProtolockDefinition;
import no.entur.schema2proto.generateproto.compatibility.protolock.ProtolockDefinitions;
import no.entur.schema2proto.generateproto.compatibility.protolock.ProtolockEnum;
import no.entur.schema2proto.generateproto.compatibility.protolock.ProtolockEnumField;
import no.entur.schema2proto.generateproto.compatibility.protolock.ProtolockFile;

public class ProtoLockParserTest {

	@Test
	public void testParseProtoLock() throws FileNotFoundException {
		ProtolockBackwardsCompatibilityChecker checker = new ProtolockBackwardsCompatibilityChecker();
		checker.init(new File("src/test/resources/proto.lock"));

		ProtolockDefinitions definitions = checker.getDefinitions();

		assertNotNull(definitions);
		assertNotNull(definitions.getDefinitions());
		assertEquals(2, definitions.getDefinitions().length);

		ProtolockDefinition def = definitions.getDefinitions()[0];
		assertEquals("net:/:opengis:/:www:/:gml:/:_3_2:/:net_opengis_www_gml__3_2.proto", def.getProtopath());
		ProtolockFile file = def.getFile();
		assertNotNull(file);

		// Check enums
		ProtolockEnum[] enums = file.getEnums();
		assertNotNull(enums);
		assertEquals(2, enums.length);

		ProtolockEnum e = enums[0];
		assertEquals("AggregationType", e.getName());
		ProtolockEnumField[] enumFields = e.getEnumFields();
		assertNotNull(enumFields);
		assertEquals(7, enumFields.length);

		assertEquals(0, enumFields[0].getInteger());
		assertEquals("AGGREGATION_TYPE_UNSPECIFIED", enumFields[0].getName());
		// Check messages

	}

}
