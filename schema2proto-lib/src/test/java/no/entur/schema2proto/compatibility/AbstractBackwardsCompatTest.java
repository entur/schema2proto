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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;

import org.apache.commons.io.FileUtils;

import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.Schema;

import no.entur.schema2proto.generateproto.wire.MutableProtoFile;
import no.entur.schema2proto.generateproto.wire.WireBuilders;
import no.entur.schema2proto.generateproto.wire.WireSchemaLoader;

public abstract class AbstractBackwardsCompatTest {

	private static final String testdataBaseDirectory = "src/test/resources/protolock";
	private static final String lockFile = "proto.lock";
	private static final String sourceFolder = "source";
	private static final String expectedFolder = "expected";

	protected void verify(String testname, boolean failOnRemovedFields, String protoFile) throws IOException {
		ProtolockBackwardsCompatibilityChecker checker = new ProtolockBackwardsCompatibilityChecker();
		File sourceDir = new File(testdataBaseDirectory + "/" + testname + "/" + sourceFolder);
		checker.init(new File(sourceDir, lockFile));

		Schema sourceSchema = loadSchema(sourceDir);
		ProtoFile sourceProtofile = sourceSchema.protoFile(protoFile);

		// schema2proto resolves backwards incompatibilities on its mutable builder model
		MutableProtoFile sourceBuilder = WireBuilders.fromProtoFile(sourceProtofile);
		boolean backwardsIncompatibiltyDetected = checker.resolveBackwardIncompatibilities(sourceBuilder);

		String generated = sourceBuilder.toSchema();

		// Verify the mutated schema still links (is valid proto)
		assertTrue(linksOk(sourceDir, protoFile, generated, testname), "Resolved schema does not link");

		Schema expectedSchema = loadSchema(new File(testdataBaseDirectory + "/" + testname + "/" + expectedFolder));
		ProtoFile expectedProtofile = expectedSchema.protoFile(protoFile);

		// Ignore the leading "// <path>" location comment, which is the only expected difference
		assertEquals(stripLocationComment(expectedProtofile.toSchema()), stripLocationComment(generated));

		if (failOnRemovedFields) {
			assertFalse(backwardsIncompatibiltyDetected);
		}
	}

	private Schema loadSchema(File path) throws IOException {
		return WireSchemaLoader.load(Collections.singletonList(path.toPath()), Collections.emptyList());
	}

	private boolean linksOk(File sourceDir, String protoFile, String generated, String testname) throws IOException {
		File tmp = new File("target/compatibilitytest_dump/" + testname);
		FileUtils.deleteDirectory(tmp);
		FileUtils.copyDirectory(sourceDir, tmp);
		File target = new File(tmp, protoFile);
		target.getParentFile().mkdirs();
		try (Writer writer = new FileWriter(target)) {
			writer.write(generated);
		}
		try {
			WireSchemaLoader.load(Collections.singletonList(tmp.toPath()), Collections.emptyList());
			return true;
		} catch (Exception e) {
			System.out.println("Linking failed, the proto file is not valid: " + e);
			return false;
		}
	}

	private static String stripLocationComment(String schema) {
		int newline = schema.indexOf('\n');
		if (schema.startsWith("//") && newline != -1) {
			return schema.substring(newline + 1);
		}
		return schema;
	}

}
