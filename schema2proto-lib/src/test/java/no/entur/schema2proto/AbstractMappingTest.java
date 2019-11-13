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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.opentest4j.AssertionFailedError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.difflib.DiffUtils;
import com.github.difflib.algorithm.DiffException;
import com.github.difflib.patch.AbstractDelta;
import com.github.difflib.patch.Patch;

import no.entur.schema2proto.generateproto.Schema2Proto;
import no.entur.schema2proto.modifyproto.InvalidProtobufException;
import no.entur.schema2proto.modifyproto.ModifyProto;
import no.entur.schema2proto.modifyproto.config.MergeFrom;
import no.entur.schema2proto.modifyproto.config.ModifyProtoConfiguration;
import no.entur.schema2proto.modifyproto.config.NewEnumConstant;
import no.entur.schema2proto.modifyproto.config.NewField;

public abstract class AbstractMappingTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMappingTest.class);
	private static final Logger FILE_CONTENT_LOGGER = LoggerFactory.getLogger("FILECONTENT");

	protected File generatedRootFolder = new File("target/generated-proto");

	protected void compareExpectedAndGenerated(File expectedRootFolder, String expectedRelativeToRootFolder, File generatedRootFolder,
			String generatedRelativeToRootFolder) {
		try {
			File e = new File(expectedRelativeToRootFolder);
			File g = new File(generatedRelativeToRootFolder);
			ProtoComparator.compareProtoFiles(expectedRootFolder, e, generatedRootFolder, g);
		} catch (AssertionFailedError e1) {
			showDiff(expectedRootFolder, generatedRootFolder, expectedRelativeToRootFolder, generatedRelativeToRootFolder);
			throw e1;
		}
	}

	private void showDiff(File expectedRootFolder, File generatedRootFolder, String expected, String generated) {
		try {
			List<String> expectedFileLines = linesFromFile(expectedRootFolder, expected);
			List<String> generatedFileLines = linesFromFile(generatedRootFolder, generated);

			Patch<String> patch = DiffUtils.diff(expectedFileLines, generatedFileLines);

			// simple output the computed patch to console
			if (patch.getDeltas().size() > 0) {

				dumpFile(expected, expectedFileLines);
				dumpFile(generated, generatedFileLines);

				FILE_CONTENT_LOGGER.info("Diff between {} and {}", expected, generated);
				for (AbstractDelta<String> delta : patch.getDeltas()) {
					FILE_CONTENT_LOGGER.info(delta.getSource() + " <----> " + delta.getTarget());
				}
				FILE_CONTENT_LOGGER.info("Diff end");
			}

		} catch (DiffException | IOException e1) {
			LOGGER.error("Error creating diff of files", e1);
		}
	}

	private void dumpFile(String filename, List<String> exlines) {
		FILE_CONTENT_LOGGER.info("****** START " + filename + " ******");
		int i = 0;
		for (String s : exlines) {
			FILE_CONTENT_LOGGER.info("[" + StringUtils.leftPad("" + i++, 4, " ") + "] " + s);
		}
		FILE_CONTENT_LOGGER.info("****** END   " + filename + " ******");
	}

	private List<String> linesFromFile(File folder, String file) throws IOException {
		List<String> lines = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new FileReader(new File(folder, file)));
		String line = null;
		while ((line = reader.readLine()) != null) {
			lines.add(line);
		}
		reader.close();
		return lines;
	}

	protected void generateProtobufNoOptions(String xsdFile) throws IOException {

		Map<String, Object> options = new HashMap<>();
		generateProtobuf(xsdFile, null, null, "default", false, options);
	}

	protected void generateProtobufNoTypeOrNameMappings(String xsdFile, Map<String, Object> additionalOptions) throws IOException {
		generateProtobuf(xsdFile, null, null, "default", false, additionalOptions);
	}

	protected void generateProtobuf(String xsdFile, String typeMappings, String nameMappings, String forcePackageName, boolean inheritanceToComposition,
			Map<String, Object> additionalOptions) throws IOException {

		FileUtils.deleteDirectory(generatedRootFolder);
		generatedRootFolder.mkdirs();

		List<String> args = new ArrayList<>();
		args.add("--outputDirectory=" + generatedRootFolder.getPath());
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

		for (Map.Entry<String, Object> entry : additionalOptions.entrySet()) {
			args.add("--" + entry.getKey() + "=" + entry.getValue());
		}

		args.add("src/test/resources/xsd/" + xsdFile);

		new Schema2Proto(args.toArray(new String[0]));

	}

	public void modifyProto(File sourceProtoFolder, List<String> includes, List<String> excludes, List<NewField> newFields, List<MergeFrom> mergeFrom,
			List<NewEnumConstant> newEnumValues) throws IOException, InvalidProtobufException {

		FileUtils.deleteDirectory(generatedRootFolder);
		generatedRootFolder.mkdirs();

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.outputDirectory = generatedRootFolder;
		configuration.inputDirectory = sourceProtoFolder;

		if (excludes != null) {
			configuration.excludes = excludes;
		}
		if (includes != null) {
			configuration.includes = includes;
		}
		if (newFields != null) {
			configuration.newFields = newFields;
		}
		if (mergeFrom != null) {
			configuration.mergeFrom = mergeFrom;
		}
		if (newEnumValues != null) {
			configuration.newEnumConstants = newEnumValues;
		}

		ModifyProto processor = new ModifyProto();
		processor.modifyProto(configuration);

	}

}
