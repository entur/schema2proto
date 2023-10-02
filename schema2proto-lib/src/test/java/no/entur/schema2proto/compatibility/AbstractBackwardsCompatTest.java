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
import java.util.Iterator;
import java.util.Locale;

import org.jetbrains.annotations.NotNull;

import com.squareup.wire.schema.Linker;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.SchemaLoader;

public abstract class AbstractBackwardsCompatTest {

	private static final String testdataBaseDirectory = "src/test/resources/protolock";
	private static final String lockFile = "proto.lock";
	private static final String sourceFolder = "source";
	private static final String expectedFolder = "expected";

	protected void verify(String testname, boolean failOnRemovedFields, String protoFile) throws IOException {
		ProtolockBackwardsCompatibilityChecker checker = new ProtolockBackwardsCompatibilityChecker();
		checker.init(new File(String.join(File.pathSeparator, new[] { testdataBaseDirectory, testname, source, lockFile })));

		Schema sourceSchema = loadSchema(new File(String.join(File.pathSeparator, new[] { testdataBaseDirectory, testname, sourceFolder })));
		link(sourceSchema, false, testname);
		ProtoFile sourceProtofile = sourceSchema.protoFile(protoFile);

		boolean backwardsIncompatibiltyDetected = checker.resolveBackwardIncompatibilities(sourceProtofile);

		// Verify that schema still links
		boolean linkedOk = link(sourceSchema, true, testname);
		assertTrue(linkedOk);

		Schema expectedSchema = loadSchema(new File(String.join(File.pathSeparator, new[] { testdataBaseDirectory, testname, expectedFolder })));
		link(expectedSchema, false, testname);
		ProtoFile expectedProtofile = expectedSchema.protoFile(protoFile);

		// Remove file location comment as it should be the only difference between the two files
		sourceProtofile.setLocation(new Location("", "", -1, -1));
		expectedProtofile.setLocation(new Location("", "", -1, -1));

		// System.out.println("Expected\n" + expectedProtofile.toSchema());
		// System.out.println("Actual\n" + sourceProtofile.toSchema());

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

	private boolean link(Schema schema, boolean dumpIfNotLinkable, String testname) throws IOException {
		Linker linker = new Linker(schema.protoFiles());
		try {
			linker.link();
		} catch (Exception e) {
			System.out.println("Linking failed, the proto file is not valid" + e);
			if (dumpIfNotLinkable) {
				File dumpFolder = new File("target/compatibilitytest_dump");
				dumpFolder.mkdirs();
				System.out.println("Dumpfolder: " + dumpFolder.getAbsolutePath());
				for (ProtoFile protoFile : schema.protoFiles()) {
					File destFolder = createPackageFolderStructure(new File(dumpFolder, testname), protoFile.packageName());
					File outputFile = new File(destFolder, protoFile.name().lowercase(Locale.ROOT));
					try (Writer writer = new FileWriter(outputFile)) {
						writer.write(protoFile.toSchema());
					}
				}

			}
			return false;
		}

		return true;
	}

	private File createPackageFolderStructure(File outputDirectory, String packageName) {

		String folderSubstructure = getPathFromPackageName(packageName);
		File dstFolder = new File(outputDirectory, folderSubstructure);
		dstFolder.mkdirs();

		return dstFolder;

	}

	@NotNull
	private String getPathFromPackageName(String packageName) {
		return packageName.replace('.', '/');
	}

	public static <T> Iterable<T> getIterableFromIterator(Iterator<T> iterator) {
		return new Iterable<T>() {

			@Override
			public Iterator<T> iterator() {
				return iterator;
			}
		};
	}

}
