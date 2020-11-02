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
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.File;
import java.io.IOException;

import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.SchemaLoader;

public abstract class AbstractBackwardsCompatTest {

	private static final String testdataBaseDirectory = "src/test/resources/protolock";
	private static final String lockFile = "proto.lock";
	private static final String sourceFolder = "source";
	private static final String expectedFolder = "expected";

	protected void verify(String testname, boolean failOnRemovedFields) throws IOException {
		ProtolockBackwardsCompatibilityChecker checker = new ProtolockBackwardsCompatibilityChecker();
		checker.init(new File(testdataBaseDirectory + "/" + testname + "/" + sourceFolder + "/" + lockFile));

		Schema sourceSchema = loadSchema(new File(testdataBaseDirectory + "/" + testname + "/" + sourceFolder));
		ProtoFile sourceProtofile = sourceSchema.protoFile("default/default.proto");

		boolean backwardsIncompatibiltyDetected = checker.resolveBackwardIncompatibilities(sourceProtofile);

		Schema expectedSchema = loadSchema(new File(testdataBaseDirectory + "/" + testname + "/" + expectedFolder));

		ProtoFile expectedProtofile = expectedSchema.protoFile("default/default.proto");

		// Remove file location comment as it should be the only difference between the two files
		sourceProtofile.setLocation(new Location("", "", -1, -1));
		expectedProtofile.setLocation(new Location("", "", -1, -1));

		System.out.println("Expected\n" + expectedProtofile.toSchema());
		System.out.println("Actual\n" + sourceProtofile.toSchema());

		assertEquals(expectedProtofile.toSchema(), sourceProtofile.toSchema());

		if (failOnRemovedFields) {
			assertFalse(backwardsIncompatibiltyDetected);
		}
	}

	private Schema loadSchema(File path) throws IOException {
		SchemaLoader schemaLoader = new SchemaLoader();
		schemaLoader.addSource(path);
		return schemaLoader.load();
	}

}
