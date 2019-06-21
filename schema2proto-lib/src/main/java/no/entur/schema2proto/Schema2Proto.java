
package no.entur.schema2proto;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import com.squareup.wire.schema.ProtoFile;

public class Schema2Proto {
	private static final String OPTION_OUTPUT_DIRECTORY = "outputDirectory";
	private static final String OPTION_OUTPUT_FILENAME = "outputFilename";
	private static final String OPTION_PACKAGE = "defaultProtoPackage";
	private static final String OPTION_FORCE_PACKAGE = "forceProtoPackage";
	private static final String OPTION_INCLUDE_FIELD_DOCS = "includeFieldDocs";
	private static final String OPTION_INCLUDE_MESSAGE_DOCS = "includeMessageDocs";
	private static final String OPTION_INCLUDE_SOURCE_LOCATION_IN_DOC = "includeSourceLocationInDoc";
	private static final String OPTION_INHERITANCE_TO_COMPOSITION = "inheritanceToComposition";
	// private static final String OPTION_TYPE_IN_ENUMS = "typeInEnums";
	private static final String OPTION_OPTIONS = "options";
	private static final String OPTION_CUSTOM_IMPORTS = "customImports";
	private static final String OPTION_CUSTOM_NAME_MAPPINGS = "customNameMappings";
	private static final String OPTION_CUSTOM_TYPE_MAPPINGS = "customTypeMappings";
	// private static final String OPTION_SPLIT_BY_SCHEMA = "splitBySchema";
//	private static final String OPTION_NEST_ENUMS = "nestEnums";
	private static final String OPTION_CONFIG_FILE = "configFile";

	private static final Logger LOGGER = LoggerFactory.getLogger(Schema2Proto.class);

	public Schema2Proto(String[] args) {
		Options commandLineOptions = createCommandLineOptions();

		if (args.length < 2) {
			printUsage(commandLineOptions);
		} else {
			try {

				CommandLineParser parser = new DefaultParser();
				CommandLine cmd = parser.parse(commandLineOptions, args);

				Schema2ProtoConfiguration configuration = getConfiguration(cmd);

				SchemaParser xp = new SchemaParser(configuration);

				LOGGER.info("Starting to parse {}", configuration.xsdFile);
				Map<String, ProtoFile> packageToFiles = xp.parse();

				TypeAndNameMapper pbm = new TypeAndNameMapper(configuration);
				ProtoSerializer serializer = new ProtoSerializer(configuration, pbm);
				serializer.serialize(packageToFiles);

				LOGGER.info("Done");
			} catch (InvalidConfigrationException | ParseException e) {
				LOGGER.error("Error parsing command line options: {}", e.getMessage());
				printUsage(commandLineOptions);
			} catch (InvalidXSDException e) {
				LOGGER.error("Error converting xsdFile to proto: {}", e.getMessage());
			} catch (Exception e) {
				LOGGER.error("Error parsing xsdFile", e);
			}
		}
	}

	public static void main(String[] args) {
		new Schema2Proto(args);
	}

	private static Options createCommandLineOptions() {
		Options commandLineOptions = new Options();
		commandLineOptions.addOption(Option.builder()
				.longOpt(OPTION_CONFIG_FILE)
				.desc("name of configfile specifying these parameters (instead of supplying them on the command line)")
				.required(false)
				.hasArg()
				.argName("<outputFilename>")
				.build());
		commandLineOptions.addOption(Option.builder()
				.longOpt(OPTION_PACKAGE)
				.hasArg()
				.argName("NAME")
				.desc("default proto package of the output file if no xsd target defaultProtoPackage is specified")
				.required(false)
				.build());
		commandLineOptions.addOption(
				Option.builder().longOpt(OPTION_FORCE_PACKAGE).hasArg().argName("NAME").desc("force all types in this package").required(false).build());
		commandLineOptions
				.addOption(Option.builder().longOpt(OPTION_OUTPUT_FILENAME).hasArg().argName("FILENAME").desc("name of output file").required(false).build());
		commandLineOptions.addOption(
				Option.builder().longOpt(OPTION_OUTPUT_DIRECTORY).hasArg().argName("DIRECTORYNAME").desc("path to output folder").required(false).build());
		/*
		 * commandLineOptions.addOption(Option.builder() .longOpt(OPTION_NEST_ENUMS) .hasArg() .argName("true|false")
		 * .desc("nest enum declaration within messages that reference them, defaults to false") .required(false) .build());
		 */
		/*
		 * commandLineOptions.addOption(Option.builder() .longOpt(OPTION_SPLIT_BY_SCHEMA) .hasArg() .argName("true|false")
		 * .desc("split output into defaultProtoPackage-specific files, defaults to false") .required(false) .build());
		 */
		commandLineOptions.addOption(Option.builder()
				.longOpt(OPTION_CUSTOM_TYPE_MAPPINGS)
				.hasArg()
				.argName("a:b,x:y")
				.desc("represent schema types as specific output types")
				.required(false)
				.build());
		commandLineOptions.addOption(Option.builder()
				.longOpt(OPTION_CUSTOM_NAME_MAPPINGS)
				.hasArg()
				.argName("cake:kake,...")
				.desc("translate message and field names")
				.required(false)
				.build());
		commandLineOptions.addOption(Option.builder()
				.longOpt(OPTION_OPTIONS)
				.hasArg()
				.argName("option1name:option1value,...")
				.desc("translate message and field names")
				.required(false)
				.build());
		commandLineOptions.addOption(Option.builder()
				.longOpt(OPTION_CUSTOM_IMPORTS)
				.hasArg()
				.argName("filename1,filename2,...")
				.desc("add additional imports")
				.required(false)
				.build());
		/*
		 * commandLineOptions.addOption(Option.builder() .longOpt(OPTION_TYPE_IN_ENUMS) .hasArg() .argName("true|false")
		 * .desc("include type as a prefix in enums, defaults to true") .required(false) .build());
		 */
		commandLineOptions.addOption(Option.builder()
				.longOpt(OPTION_INCLUDE_MESSAGE_DOCS)
				.hasArg()
				.argName("true|false")
				.desc("include documentation of messages in output, defaults to true")
				.required(false)
				.build());
		commandLineOptions.addOption(Option.builder()
				.longOpt(OPTION_INCLUDE_FIELD_DOCS)
				.hasArg()
				.argName("true|false")
				.desc("include documentation for fields in output, defaults to true")
				.required(false)
				.build());
		commandLineOptions.addOption(Option.builder()
				.longOpt(OPTION_INCLUDE_SOURCE_LOCATION_IN_DOC)
				.hasArg()
				.argName("true|false")
				.desc("include xsd source location in docs, defaults to true")
				.required(false)
				.build());
		commandLineOptions.addOption(Option.builder()
				.longOpt(OPTION_INHERITANCE_TO_COMPOSITION)
				.hasArg()
				.argName("true|false")
				.desc("define each xsd extension base level as a message field instead of copying all inherited fields")
				.required(false)
				.build());
		return commandLineOptions;
	}

	private static void printUsage(Options commandLineOptions) {
		HelpFormatter formatter = new HelpFormatter();

		formatter.setWidth(140);
		formatter.setSyntaxPrefix("java ");

		formatter.printHelp("Schema2Proto [OPTIONS] XSDFILE", "Generate proto files from xsd file. Either --configFile or --outputDirectory must be specified.",
				commandLineOptions, null);
	}

	private static Schema2ProtoConfiguration getConfiguration(CommandLine cmd) throws InvalidConfigrationException {

		if (cmd.hasOption(OPTION_CONFIG_FILE)) {
			return parseConfigFile(cmd);
		} else {
			return parseCommandLineOptions(cmd);
		}

	}

	private static Schema2ProtoConfiguration parseConfigFile(CommandLine cmd) throws InvalidConfigrationException {

		Schema2ProtoConfiguration configuration = new Schema2ProtoConfiguration();

		String[] args = cmd.getArgs();
		if (args.length != 1) {
			throw new InvalidConfigrationException("Missing xsd file argument");
		} else {
			File xsdFile = new File(args[0]);
			if (!xsdFile.exists()) {
				throw new InvalidConfigrationException(String.format("XSD file %s not found", xsdFile.getAbsolutePath()));
			}
			configuration.xsdFile = new File(args[0]);
		}

		String configFile = cmd.getOptionValue(OPTION_CONFIG_FILE);
		try (InputStream in = Files.newInputStream(Paths.get(configFile))) {
			LOGGER.info("Using configFile {}", configFile);
			Yaml yaml = new Yaml();
			Schema2ProtoConfigFile config = yaml.loadAs(in, Schema2ProtoConfigFile.class);

			if (config.outputDirectory == null) {
				throw new InvalidConfigrationException(OPTION_OUTPUT_DIRECTORY);
			} else {
				configuration.outputDirectory = new File(config.outputDirectory);
			}

			configuration.outputFilename = config.outputFilename;

			HashMap<Pattern, String> customTypeMappings = new LinkedHashMap<>();
			if (config.customTypeMappings != null) {
				for (Entry<String, String> kv : config.customTypeMappings.entrySet()) {
					Pattern p = Pattern.compile(kv.getKey());
					customTypeMappings.put(p, kv.getValue());
				}
			}

			HashMap<Pattern, String> customNameMappings = new LinkedHashMap<>();
			if (config.customNameMappings != null) {
				for (Entry<String, String> kv : config.customNameMappings.entrySet()) {
					Pattern p = Pattern.compile(kv.getKey());
					customNameMappings.put(p, kv.getValue());
				}
			}
			configuration.customTypeMappings.putAll(customTypeMappings);
			configuration.customNameMappings.putAll(customNameMappings);
			configuration.defaultProtoPackage = config.defaultProtoPackage;
			configuration.forceProtoPackage = config.forceProtoPackage;
			// configuration.splitBySchema = config.splitBySchema;
//			configuration.nestEnums = config.nestEnums;
//			configuration.typeInEnums = config.typeInEnums;
			configuration.includeMessageDocs = config.includeMessageDocs;
			configuration.includeFieldDocs = config.includeFieldDocs;
			configuration.includeSourceLocationInDoc = config.includeSourceLocationInDoc;
			configuration.inheritanceToComposition = config.inheritanceToComposition;

			Map<String, String> options = config.options;
			if (config.options != null) {
				configuration.options.putAll(options);
			}

			if (config.customImports != null) {
				for (String importStatment : config.customImports.split(",")) {
					configuration.customImports.add(importStatment);
				}

			}

			return configuration;

		} catch (IOException e) {
			throw new InvalidConfigrationException("Error reading config file", e);
		} catch (YAMLException e) {
			throw new InvalidConfigrationException("Error parsing config file", e);
		}
	}

	private static Schema2ProtoConfiguration parseCommandLineOptions(CommandLine cmd) throws InvalidConfigrationException {

		Schema2ProtoConfiguration configuration = new Schema2ProtoConfiguration();

		String[] args = cmd.getArgs();
		if (args.length != 1) {
			throw new InvalidConfigrationException("Missing xsd file argument");
		} else {
			configuration.xsdFile = new File(args[0]);
		}

		if (!cmd.hasOption(OPTION_OUTPUT_DIRECTORY)) {
			throw new InvalidConfigrationException(OPTION_OUTPUT_DIRECTORY);
		} else {
			configuration.outputDirectory = new File(cmd.getOptionValue(OPTION_OUTPUT_DIRECTORY));
		}

		configuration.outputFilename = cmd.getOptionValue(OPTION_OUTPUT_FILENAME);

		if (cmd.hasOption(OPTION_PACKAGE)) {
			configuration.defaultProtoPackage = cmd.getOptionValue(OPTION_PACKAGE);
		}
		if (cmd.hasOption(OPTION_FORCE_PACKAGE)) {
			configuration.forceProtoPackage = cmd.getOptionValue(OPTION_FORCE_PACKAGE);
		}
//		if (cmd.hasOption(OPTION_SPLIT_BY_SCHEMA)) {
//			configuration.splitBySchema = Boolean.parseBoolean(cmd.getOptionValue(OPTION_SPLIT_BY_SCHEMA));
//		}

		HashMap<Pattern, String> customTypeMappings = new LinkedHashMap<>();
		if (cmd.hasOption(OPTION_CUSTOM_TYPE_MAPPINGS)) {
			for (String mapping : cmd.getOptionValue(OPTION_CUSTOM_TYPE_MAPPINGS).split(",")) {
				int colon = mapping.indexOf(':');
				if (colon > -1) {
					customTypeMappings.put(Pattern.compile(mapping.substring(0, colon)), mapping.substring(colon + 1));
				} else {
					LOGGER.error(mapping + " is not a valid custom type mapping - use schematype:outputtype");
				}
			}
		}
		configuration.customTypeMappings = customTypeMappings;

		HashMap<Pattern, String> customNameMappings = new LinkedHashMap<>();
		if (cmd.hasOption(OPTION_CUSTOM_NAME_MAPPINGS)) {
			for (String mapping : cmd.getOptionValue(OPTION_CUSTOM_NAME_MAPPINGS).split(",")) {
				int colon = mapping.indexOf(':');
				if (colon > -1) {
					customNameMappings.put(Pattern.compile(mapping.substring(0, colon)), mapping.substring(colon + 1));
				} else {
					LOGGER.error(mapping + " is not a valid custom name mapping - use xsdelementname:protomessagename/protofieldname");
				}
			}
		}
		configuration.customNameMappings = customNameMappings;

		HashMap<String, String> options = new LinkedHashMap<>();
		if (cmd.hasOption(OPTION_OPTIONS)) {
			for (String mapping : cmd.getOptionValue(OPTION_OPTIONS).split(",")) {
				int colon = mapping.indexOf(':');
				if (colon > -1) {
					options.put(mapping.substring(0, colon), mapping.substring(colon + 1));
				} else {
					LOGGER.error(mapping + " is not a option, use optionName:optionValue");
				}
			}
		}
		configuration.options = options;

		List<String> imports = new ArrayList<>();
		if (cmd.hasOption(OPTION_CUSTOM_IMPORTS)) {
			for (String mapping : cmd.getOptionValue(OPTION_CUSTOM_IMPORTS).split(",")) {
				imports.add(mapping);
			}
		}
		configuration.customImports = imports;

//		if (cmd.hasOption(OPTION_NEST_ENUMS)) {
//			configuration.nestEnums = Boolean.parseBoolean(cmd.getOptionValue(OPTION_NEST_ENUMS));
//		}
//		if (cmd.hasOption(OPTION_TYPE_IN_ENUMS)) {
//			configuration.typeInEnums = Boolean.parseBoolean(cmd.getOptionValue(OPTION_TYPE_IN_ENUMS));
//		}
		if (cmd.hasOption(OPTION_INCLUDE_MESSAGE_DOCS)) {
			configuration.includeMessageDocs = Boolean.parseBoolean(cmd.getOptionValue(OPTION_INCLUDE_MESSAGE_DOCS));
		}
		if (cmd.hasOption(OPTION_INCLUDE_FIELD_DOCS)) {
			configuration.includeFieldDocs = Boolean.parseBoolean(cmd.getOptionValue(OPTION_INCLUDE_FIELD_DOCS));
		}
		if (cmd.hasOption(OPTION_INCLUDE_SOURCE_LOCATION_IN_DOC)) {
			configuration.includeSourceLocationInDoc = Boolean.parseBoolean(cmd.getOptionValue(OPTION_INCLUDE_SOURCE_LOCATION_IN_DOC));
		}
		if (cmd.hasOption(OPTION_INHERITANCE_TO_COMPOSITION)) {
			configuration.inheritanceToComposition = Boolean.parseBoolean(cmd.getOptionValue(OPTION_INHERITANCE_TO_COMPOSITION));
		}

		return configuration;
	}
}
