package no.entur.schema2proto;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

	public static void compareExpectedAndGenerated(String expected, String generated) throws IOException {

		File e = new File(expected);
		File g = new File(generated);

		List<String> exlines = linesFromFile(expected);
		List<String> genlines = linesFromFile(generated);

		try {
			ProtoComparator.compareProtoFiles(e, g);
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

	public static String generateProtobuf(String name, String typeMappings, String nameMappings, String packageName) {
		return generate(name, "protobuf", "proto", typeMappings, nameMappings, packageName);
	}

	public static String generateProtobuf(String name) {
		return generate(name, "protobuf", "proto", null, null, "default");
	}

	private static String generate(String name, String type, String extension, String typeMappings, String nameMappings, String packageName) {
		File dir = new File("target/generated-proto/");
		if (!dir.exists())
			dir.mkdir();
		String filename = "target/generated-proto/" + name + "." + extension;

		List<String> args = new ArrayList<>();
		args.add("--filename=" + filename);
		args.add("--package=" + packageName);
		if (typeMappings != null) {
			args.add("--customTypeMappings=" + typeMappings);
		}
		if (nameMappings != null) {
			args.add("--customNameMappings=" + nameMappings);
		}
		args.add("src/test/resources/xsd/" + name + ".xsd");

		Schema2Proto.main(args.toArray(new String[0]));

		return filename;
	}

	private static List<String> linesFromFile(String file) throws IOException {
		List<String> lines = new ArrayList<String>();
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = null;
		while ((line = reader.readLine()) != null) {
			lines.add(line);
		}
		reader.close();
		return lines;
	}

}
