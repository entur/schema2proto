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

package no.entur.schema2proto.generateproto;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import com.squareup.wire.schema.ProtoFile;

import no.entur.schema2proto.InvalidConfigurationException;

public class Schema2Proto {
	private static final String OPTION_OUTPUT_DIRECTORY = "outputDirectory";
	private static final String OPTION_OUTPUT_FILENAME = "outputFilename";
	private static final String OPTION_PACKAGE = "defaultProtoPackage";
	private static final String OPTION_FORCE_PACKAGE = "forceProtoPackage";
	private static final String OPTION_INCLUDE_FIELD_DOCS = "includeFieldDocs";
	private static final String OPTION_INCLUDE_MESSAGE_DOCS = "includeMessageDocs";
	private static final String OPTION_INCLUDE_SOURCE_LOCATION_IN_DOC = "includeSourceLocationInDoc";
	private static final String OPTION_INHERITANCE_TO_COMPOSITION = "inheritanceToComposition";
	private static final String OPTION_OPTIONS = "options";
	private static final String OPTION_CUSTOM_IMPORTS = "customImports";
	private static final String OPTION_CUSTOM_IMPORT_LOCATIONS = "customImportLocations";
	private static final String OPTION_CUSTOM_NAME_MAPPINGS = "customNameMappings";
	private static final String OPTION_CUSTOM_TYPE_MAPPINGS = "customTypeMappings";
	private static final String OPTION_CUSTOM_TYPE_REPLACINGS = "customTypeReplacements";
	private static final String OPTION_IGNORE_OUTPUT_FIELDS = "ignoreOutputFields";
	private static final String OPTION_CONFIG_FILE = "configFile";
	private static final String OPTION_INCLUDE_VALIDATION_RULES = "includeValidationRules";
	private static final String OPTION_INCLUDE_SKIP_EMPTY_TYPE_INHERITANCE = "skipEmptyTypeInheritance";
	private static final String OPTION_INCLUDE_XSD_OPTIONS = "includeXsdOptions";
	private static final String OPTION_PROTOLOCK_FILENAME = "protoLockFile";
	private static final String OPTION_FAIL_IF_REMOVED_FIELDS = "failIfRemovedFields";
	private static final String OPTION_DERIVATION_BY_SUBSUMPTION = "derivationBySubsumption";
	private static final String OPTION_INCLUDE_GO_PACKAGE_OPTIONS = "includeGoPackageOptions";
	private static final String OPTION_GO_PACKAGE_SOURCE_PREFIX = "goPackageSourcePrefix";
	private static final Logger LOGGER = LoggerFactory.getLogger(Schema2Proto.class);

	public Schema2Proto(String[] args) throws IOException {
		Options commandLineOptions = createCommandLineOptions();

		if (args.length < 2) {
			printUsage(commandLineOptions);
		} else {
			try {
				CommandLineParser parser = new DefaultParser();
				CommandLine cmd = parser.parse(commandLineOptions, args);
				Schema2ProtoConfiguration configuration = getConfiguration(cmd);
				parseAndSerialize(configuration);
			} catch (InvalidConfigurationException | ParseException e) {
				printUsage(commandLineOptions);
				throw new ConversionException("Error parsing command line options", e);
			} catch (com.squareup.wire.schema.SchemaException e) {
				throw new ConversionException("Generated proto files did not link", e);
			}
		}
	}

	public static void parseAndSerialize(Schema2ProtoConfiguration configuration) throws IOException, InvalidConfigurationException {
		try {
			SchemaParser xp = new SchemaParser(configuration);

			LOGGER.info("Starting to parse {}", configuration.xsdFile);
			Map<String, ProtoFile> packageToFiles = xp.parse();
			List<LocalType> localTypes = xp.getLocalTypes();

			TypeAndNameMapper pbm = new TypeAndNameMapper(configuration);
			ProtoSerializer serializer = new ProtoSerializer(configuration, pbm);
			serializer.serialize(packageToFiles, localTypes);

			LOGGER.info("Done");
		} catch (InvalidXSDException e) {
			throw new ConversionException("Error converting xsdFile to proto", e);
		} catch (SAXException e) {
			throw new ConversionException("Error parsing provided xsd. Correct xsd and retry", e);
		}

	}

	public static void main(String[] args) {
		try {
			new Schema2Proto(args);
		} catch (Exception e) {
			LOGGER.error("Error processing proto files: {}", e.getMessage(), e);
			System.exit(1);
		}
	}

	private static Options createCommandLineOptions() {
		Options commandLineOptions = new Options();
		commandLineOptions.addOption(Option.builder()
				.longOpt(OPTION_CONFIG_FILE)
				.desc("name of configfile specifying these parameters (instead of supplying them on the command line)")
				.required(false)
				.hasArg()
				.argName("outputFilename")
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
				.desc("add custom options to each protofile, ie java_multiple_files:true")
				.required(false)
				.build());
		commandLineOptions.addOption(Option.builder()
				.longOpt(OPTION_CUSTOM_IMPORTS)
				.hasArg()
				.argName("filename1,filename2,...")
				.desc("add additional imports")
				.required(false)
				.build());
		commandLineOptions.addOption(Option.builder()
				.longOpt(OPTION_CUSTOM_IMPORT_LOCATIONS)
				.hasArg()
				.argName("folder1,folder2,...")
				.desc("root folder for additional imports")
				.required(false)
				.build());
		commandLineOptions.addOption(Option.builder()
				.longOpt(OPTION_IGNORE_OUTPUT_FIELDS)
				.hasArg()
				.argName("packageName1/messageName1/fieldName1, packageName2/...")
				.desc("output field names to ignore")
				.required(false)
				.build());
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
				.desc("include xsd source location relative to source xsd file in docs, defaults to false")
				.required(false)
				.build());
		commandLineOptions.addOption(Option.builder()
				.longOpt(OPTION_INHERITANCE_TO_COMPOSITION)
				.hasArg()
				.argName("true|false")
				.desc("define each xsd extension base level as a message field instead of copying all inherited fields")
				.required(false)
				.build());
		commandLineOptions.addOption(Option.builder()
				.longOpt(OPTION_INCLUDE_VALIDATION_RULES)
				.hasArg()
				.argName("true|false")
				.desc("generate envoypropxy/protoc-gen-validate validation rules from xsd rules")
				.required(false)
				.build());
		commandLineOptions.addOption(Option.builder()
				.longOpt(OPTION_INCLUDE_SKIP_EMPTY_TYPE_INHERITANCE)
				.hasArg()
				.argName("true|false")
				.desc("skip types just redefining other types with a different name")
				.required(false)
				.build());
		commandLineOptions.addOption(Option.builder()
				.longOpt(OPTION_INCLUDE_XSD_OPTIONS)
				.hasArg()
				.argName("true|false")
				.desc("include message options describing the xsd type hierarchy")
				.required(false)
				.build());
		commandLineOptions.addOption(Option.builder()
				.longOpt(OPTION_FAIL_IF_REMOVED_FIELDS)
				.hasArg()
				.argName("true|false")
				.desc("when using backwards compatibility check via proto.lock file, fail if proto fields are removed")
				.required(false)
				.build());
		commandLineOptions.addOption(
				Option.builder().longOpt(OPTION_PROTOLOCK_FILENAME).hasArg().argName("FILENAME").desc("Full path to proto.lock file").required(false).build());
		commandLineOptions.addOption(Option.builder()
				.longOpt(OPTION_DERIVATION_BY_SUBSUMPTION)
				.hasArg()
				.argName("true|false")
				.desc("enable derivation by subsumption https://cs.au.dk/~amoeller/XML/schemas/xmlschema-inheritance.html")
				.required(false)
				.build());
		commandLineOptions.addOption(Option.builder()
				.longOpt(OPTION_INCLUDE_GO_PACKAGE_OPTIONS)
				.hasArg()
				.argName("true|false")
				.desc("Include 'go_package' options in all files")
				.required(false)
				.build());
		commandLineOptions.addOption(Option.builder()
				.longOpt(OPTION_GO_PACKAGE_SOURCE_PREFIX)
				.hasArg()
				.argName("google.golang.org/protobuf/types/known/")
				.desc("Source path to set as prefix for go_package options")
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

	private static Schema2ProtoConfiguration getConfiguration(CommandLine cmd) throws InvalidConfigurationException {

		if (cmd.hasOption(OPTION_CONFIG_FILE)) {
			return parseConfigFile(cmd);
		} else {
			return parseCommandLineOptions(cmd);
		}

	}

	private static Schema2ProtoConfiguration parseConfigFile(CommandLine cmd) throws InvalidConfigurationException {

		Schema2ProtoConfiguration configuration = new Schema2ProtoConfiguration();

		String[] args = cmd.getArgs();
		if (args.length != 1) {
			throw new InvalidConfigurationException("Missing xsd file argument");
		} else {
			File xsdFile = new File(args[0]);
			if (!xsdFile.exists()) {
				throw new InvalidConfigurationException(String.format("XSD file %s not found", xsdFile.getAbsolutePath()));
			}
			configuration.xsdFile = new File(args[0]);
		}

		String configFileOption = cmd.getOptionValue(OPTION_CONFIG_FILE);
		try (InputStream in = Files.newInputStream(Paths.get(configFileOption))) {
			LOGGER.info("Using configFile {}", configFileOption);

			parseConfigurationFileIntoConfiguration(configuration, in);

			return configuration;

		} catch (IOException e) {
			throw new InvalidConfigurationException("Error reading config file", e);
		} catch (YAMLException e) {
			throw new InvalidConfigurationException("Error parsing config file", e);
		}
	}

	public static void parseConfigurationFileIntoConfiguration(Schema2ProtoConfiguration configuration, InputStream in) throws InvalidConfigurationException {
		Yaml yaml = new Yaml();
		Schema2ProtoConfigFile configFile = yaml.loadAs(in, Schema2ProtoConfigFile.class);

		if (configFile.outputDirectory == null) {
			throw new InvalidConfigurationException(OPTION_OUTPUT_DIRECTORY);
		} else {
			configuration.outputDirectory = new File(configFile.outputDirectory);
		}

		configuration.outputFilename = configFile.outputFilename;

		Map<Pattern, String> customTypeMappings = new LinkedHashMap<>();
		if (configFile.customTypeMappings != null) {
			for (Entry<String, String> kv : configFile.customTypeMappings.entrySet()) {
				Pattern p = Pattern.compile(kv.getKey());
				customTypeMappings.put(p, kv.getValue());
			}
		}

		Map<Pattern, String> customTypeReplacements = new LinkedHashMap<>();
		if (configFile.customTypeReplacements != null) {
			for (Entry<String, String> kv : configFile.customTypeReplacements.entrySet()) {
				Pattern p = Pattern.compile(kv.getKey());
				customTypeReplacements.put(p, kv.getValue());
			}
		}

		Map<Pattern, String> customNameMappings = new LinkedHashMap<>();
		if (configFile.customNameMappings != null) {
			for (Entry<String, String> kv : configFile.customNameMappings.entrySet()) {
				Pattern p = Pattern.compile(kv.getKey());
				customNameMappings.put(p, kv.getValue());
			}
		}
		configuration.customTypeMappings.putAll(customTypeMappings);
		configuration.customTypeReplacements.putAll(customTypeReplacements);
		configuration.customNameMappings.putAll(customNameMappings);
		configuration.defaultProtoPackage = configFile.defaultProtoPackage;
		configuration.forceProtoPackage = configFile.forceProtoPackage;
		configuration.includeMessageDocs = configFile.includeMessageDocs;
		configuration.includeFieldDocs = configFile.includeFieldDocs;
		configuration.includeSourceLocationInDoc = configFile.includeSourceLocationInDoc;
		configuration.inheritanceToComposition = configFile.inheritanceToComposition;
		configuration.includeValidationRules = configFile.includeValidationRules;
		configuration.skipEmptyTypeInheritance = configFile.skipEmptyTypeInheritance;
		configuration.includeXsdOptions = configFile.includeXsdOptions;
		configuration.derivationBySubsumption = configFile.derivationBySubsumption;
		configuration.includeGoPackageOptions = configFile.includeGoPackageOptions;
		configuration.goPackageSourcePrefix = configFile.goPackageSourcePrefix;

		Map<String, Object> options = configFile.options;
		if (configFile.options != null) {
			configuration.options.putAll(options);
		}

		if (configFile.customImports != null) {
			for (String importStatment : configFile.customImports) {
				configuration.customImports.add(importStatment);
			}

		}
		if (configFile.customImportLocations != null) {
			for (String importLocation : configFile.customImportLocations) {
				configuration.customImportLocations.add(importLocation);
			}
		}

		if (configFile.ignoreOutputFields != null) {
			for (String ignoreOutputField : configFile.ignoreOutputFields) {
				String[] split = StringUtils.split(ignoreOutputField, "/");
				configuration.ignoreOutputFields.add(new FieldPath(split[0], split[1], split[2]));
			}
		}

		if (configFile.protoLockFile != null) {
			configuration.protoLockFile = new File(configFile.protoLockFile);
		}

		configuration.failIfRemovedFields = configFile.failIfRemovedFields;
	}

	private static Schema2ProtoConfiguration parseCommandLineOptions(CommandLine cmd) throws InvalidConfigurationException {

		Schema2ProtoConfiguration configuration = new Schema2ProtoConfiguration();

		String[] args = cmd.getArgs();
		if (args.length != 1) {
			throw new InvalidConfigurationException("Missing xsd file argument");
		} else {
			configuration.xsdFile = new File(args[0]);
		}

		if (!cmd.hasOption(OPTION_OUTPUT_DIRECTORY)) {
			throw new InvalidConfigurationException(OPTION_OUTPUT_DIRECTORY);
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

		configuration.customTypeMappings = parseRegexMap(cmd, OPTION_CUSTOM_TYPE_MAPPINGS);
		configuration.customTypeReplacements = parseRegexMap(cmd, OPTION_CUSTOM_TYPE_REPLACINGS);
		configuration.customNameMappings = parseRegexMap(cmd, OPTION_CUSTOM_NAME_MAPPINGS);

		HashMap<String, Object> options = new LinkedHashMap<>();
		if (cmd.hasOption(OPTION_OPTIONS)) {
			for (String mapping : cmd.getOptionValue(OPTION_OPTIONS).split(",")) {
				int colon = mapping.indexOf(':');
				if (colon > -1) {
					String optionName = mapping.substring(0, colon);
					String parameter = mapping.substring(colon + 1);
					if (parameter.equals("true")) {
						options.put(optionName, true);
					} else if (parameter.equals("false")) {
						options.put(optionName, false);
					} else {
						options.put(optionName, parameter);
					}
				} else {
					LOGGER.error("{} is not a option, use optionName:optionValue", mapping);
				}
			}
		}

		configuration.options = options;
		configuration.customImports = parseCommaSeparatedStringValues(cmd, OPTION_CUSTOM_IMPORTS);
		configuration.customImportLocations = parseCommaSeparatedStringValues(cmd, OPTION_CUSTOM_IMPORT_LOCATIONS);

		if (cmd.hasOption(OPTION_IGNORE_OUTPUT_FIELDS)) {
			for (String ignoreOutputField : cmd.getOptionValue(OPTION_IGNORE_OUTPUT_FIELDS).split(",")) {
				String[] split = StringUtils.split(ignoreOutputField, "/");
				configuration.ignoreOutputFields.add(new FieldPath(split[0], split[1], split[2]));
			}
		}

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
		if (cmd.hasOption(OPTION_INCLUDE_VALIDATION_RULES)) {
			configuration.includeValidationRules = Boolean.parseBoolean(cmd.getOptionValue(OPTION_INCLUDE_VALIDATION_RULES));
		}
		if (cmd.hasOption(OPTION_INCLUDE_SKIP_EMPTY_TYPE_INHERITANCE)) {
			configuration.skipEmptyTypeInheritance = Boolean.parseBoolean(cmd.getOptionValue(OPTION_INCLUDE_SKIP_EMPTY_TYPE_INHERITANCE));
		}
		if (cmd.hasOption(OPTION_INCLUDE_XSD_OPTIONS)) {
			configuration.includeXsdOptions = Boolean.parseBoolean(cmd.getOptionValue(OPTION_INCLUDE_XSD_OPTIONS));
		}
		if (cmd.hasOption(OPTION_PROTOLOCK_FILENAME)) {
			configuration.protoLockFile = new File(cmd.getOptionValue(OPTION_PROTOLOCK_FILENAME));
		}
		if (cmd.hasOption(OPTION_DERIVATION_BY_SUBSUMPTION)) {
			configuration.derivationBySubsumption = Boolean.parseBoolean(cmd.getOptionValue(OPTION_DERIVATION_BY_SUBSUMPTION));
		}
		if (cmd.hasOption(OPTION_INCLUDE_GO_PACKAGE_OPTIONS)) {
			configuration.includeGoPackageOptions = Boolean.parseBoolean(cmd.getOptionValue(OPTION_INCLUDE_GO_PACKAGE_OPTIONS));
		}
		if (cmd.hasOption(OPTION_GO_PACKAGE_SOURCE_PREFIX)) {
			configuration.goPackageSourcePrefix = cmd.getOptionValue(OPTION_GO_PACKAGE_SOURCE_PREFIX);
		}

		return configuration;
	}

	private static List<String> parseCommaSeparatedStringValues(CommandLine cmd, String optionName) {
		List<String> imports = new ArrayList<>();
		if (cmd.hasOption(optionName)) {
			for (String mapping : cmd.getOptionValue(optionName).split(",")) {
				imports.add(mapping);
			}
		}
		return imports;
	}

	private static HashMap<Pattern, String> parseRegexMap(CommandLine cmd, String optionName) {
		HashMap<Pattern, String> customTypeMappings = new LinkedHashMap<>();
		if (cmd.hasOption(optionName)) {
			for (String mapping : cmd.getOptionValue(optionName).split(",")) {
				int colon = mapping.indexOf(':');
				if (colon > -1) {
					customTypeMappings.put(Pattern.compile(mapping.substring(0, colon)), mapping.substring(colon + 1));
				} else {
					LOGGER.error("{} is not a valid mapping - use schematype:outputtype/fieldname", mapping);
				}
			}
		}
		return customTypeMappings;
	}
}
