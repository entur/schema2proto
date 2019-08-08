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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;

public class TestHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestHelper.class);
	private static final Logger FILECONTENTLOGGER = LoggerFactory.getLogger("FILECONTENT");

	public static void compareExpectedAndGenerated(File expectedRootFolder, String expected, File generatedRootFolder, String generated) throws IOException {

		File e = new File(expected);
		File g = new File(generated);

		List<String> exlines = linesFromFile(expectedRootFolder, expected);
		List<String> genlines = linesFromFile(generatedRootFolder, generated);

		try {
			ProtoComparator.compareProtoFiles(expectedRootFolder, e, generatedRootFolder, g);
		} catch (AssertionFailedError e1) {
			showDiff(expected, generated, exlines, genlines);

			throw e1;
		}
	}

	private static void showDiff(String expected, String generated, List<String> exlines, List<String> genlines) {
		try {
			Patch<String> patch = DiffUtils.diff(exlines, genlines);

			// simple output the computed patch to console
			if (patch.getDeltas().size() > 0) {

				dumpFile(expected, exlines);
				dumpFile(generated, genlines);

				FILECONTENTLOGGER.info("Diff between {} and {}", expected, generated);
				for (AbstractDelta<String> delta : patch.getDeltas()) {
					FILECONTENTLOGGER.info(delta.getSource() + " <----> " + delta.getTarget());
				}
				FILECONTENTLOGGER.info("Diff end");
			}

		} catch (DiffException e1) {
			LOGGER.error("Error creating diff of files", e1);
		}
	}

	private static void dumpFile(String filename, List<String> exlines) {
		FILECONTENTLOGGER.info("****** START " + filename + " ******");
		int i = 0;
		for (String s : exlines) {
			FILECONTENTLOGGER.info("[" + StringUtils.leftPad("" + i++, 4, " ") + "] " + s);
		}
		FILECONTENTLOGGER.info("****** END   " + filename + " ******");
	}

	public static String generateProtobuf(String xsdFile, String typeMappings, String nameMappings, String forcePackageName, boolean inheritanceToComposition,
			String expectedFolder, String expectedFilename) throws IOException {
		return generate(xsdFile, typeMappings, nameMappings, forcePackageName, inheritanceToComposition, expectedFolder, expectedFilename, false);
	}

	public static String generateProtobuf(String xsdFile, String expectedFolder, String expectedFilename, boolean skipEmptyTypeInheritance) throws IOException {
		return generate(xsdFile, null, null, "default", false, expectedFolder, expectedFilename, skipEmptyTypeInheritance);
	}

	private static String generate(String xsdFile, String typeMappings, String nameMappings, String forcePackageName, boolean inheritanceToComposition,
			String expectedFolder, String expectedFilename, boolean skipEmptyTypeInheritance) throws IOException {
		File dir = new File("target/generated-proto/");
		FileUtils.deleteDirectory(dir);
		dir.mkdirs();

		String filename = expectedFilename;

		List<String> args = new ArrayList<>();
		args.add("--outputDirectory=" + dir.getPath());
		if (forcePackageName != null) {
			args.add("--forceProtoPackage=" + forcePackageName);
		}
		if (typeMappings != null) {
			args.add("--customTypeMappings=" + typeMappings);
		}
		if (nameMappings != null) {
			args.add("--customNameMappings=" + nameMappings);
		}

		args.add("--inheritanceToComposition=" + inheritanceToComposition);
		args.add("--skipEmptyTypeInheritance=" + skipEmptyTypeInheritance);

		args.add("src/test/resources/xsd/" + xsdFile);

		File outputFile = new File(dir, expectedFolder + "/" + filename);
		if (outputFile.exists()) {
			outputFile.delete();
		}

		File parentFile = outputFile.getParentFile();
		parentFile.mkdirs();

		new Schema2Proto(args.toArray(new String[0]));

		return outputFile.getPath();
	}

	public static File reduce(File sourceProtoFolder, List<String> includes, List<String> excludes, List<NewField> newFields, List<MergeFrom> mergeFrom)
			throws IOException {
		File dir = new File("target/generated-proto/");
		FileUtils.deleteDirectory(dir);
		dir.mkdirs();

		ReduceProtoConfiguration configuration = new ReduceProtoConfiguration();
		configuration.excludes = excludes;
		configuration.includes = includes;
		configuration.outputDirectory = dir;
		configuration.inputDirectory = sourceProtoFolder;
		configuration.newFields = newFields;
		configuration.mergeFrom = mergeFrom;

		ReduceProto processor = new ReduceProto();
		processor.reduceProto(configuration);

		return dir;

	}

	private static List<String> linesFromFile(File folder, String file) throws IOException {
		List<String> lines = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new FileReader(new File(folder, file)));
		String line = null;
		while ((line = reader.readLine()) != null) {
			lines.add(line);
		}
		reader.close();
		return lines;
	}

}
