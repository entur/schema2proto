
package no.entur.schema2proto;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.YAMLException;

import no.entur.schema2proto.marshal.ProtobufMarshaller;

public class Schema2Proto {

	private static final Logger LOGGER = LoggerFactory.getLogger(Schema2Proto.class);

	private static boolean correct;
	private static String usage = "" + "Usage: java xsd2proto-<VERSION>.jar [--output=FILENAME]\n"
			+ "                           [--package=NAME] filename.xsd\n" + "\n" + "  --configFile=FILENAME           : path to configuration file\n"
			+ "\nOR\n" + "\n" + "  --filename=FILENAME             : store the result in FILENAME instead of standard output\n"
			+ "  --package=NAME                  : set namespace/package of the output file\n"
			+ "  --nestEnums=true|false          : nest enum declaration within messages that reference them, only supported by protobuf, defaults to true\n"
			+ "  --splitBySchema=true|false      : split output into namespace-specific files, defaults to false\n"
			+ "  --customTypeMappings=a:b,x:y    : represent schema types as specific output types\n"
			+ "  --customNameMappings=cake:kake,...: translate message and field names\n"
			+ "  --typeInEnums=true|false        : include type as a prefix in enums, defaults to true\n"
			+ "  --includeMessageDocs=true|false : include documentation of messages in output, defaults to true\n"
			+ "  --includeFieldDocs=true|false   : include documentation for fields in output, defaults to true\n" + "";

	private static void usage(String error) {
		LOGGER.error(error);
		usage();
	}

	private static void usage() {
		LOGGER.info(usage);
		correct = false;
	}

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) {
		XSDParser xp;
		HashMap<String, String> map;
		String xsd, param;
		int i;
		ProtobufMarshaller pbm = null;

		OutputWriter writer;
		correct = true;

		map = new HashMap<>();
		map.put("schema_._type", "binary");
		map.put("EString", "string");
		map.put("EBoolean", "boolean");
		map.put("EInt", "integer");
		map.put("EDate", "long");
		map.put("EChar", "byte");
		map.put("EFloat", "decimal");
		map.put("EObject", "binary");
		map.put("Extension", "binary");

		if (args.length == 0 || args[args.length - 1].startsWith("--")) {
			usage();
		} else {
			xsd = args[args.length - 1];
			xp = new XSDParser(xsd, map);
			writer = new OutputWriter();
			xp.setWriter(writer);

			pbm = new ProtobufMarshaller();

			xp.addMarshaller(pbm);
			writer.setMarshaller(pbm);
			writer.setDefaultExtension("proto");

			Map<Pattern, String> customTypeMappings = null;
			Map<Pattern, String> customNameMappings = null;
			Map<String, Object> options = null;

			if (args.length == 2 && args[0].startsWith("--configFile=")) {
				Yaml yaml = new Yaml();
				String configFile = args[0].split("=")[1];
				try (InputStream in = Files.newInputStream(Paths.get(configFile))) {
					LOGGER.info("Using configFile {}", configFile);
					ConfigFile config = yaml.loadAs(in, ConfigFile.class);

					writer.setFilename(config.filename);
					writer.setDirectory(config.directory);

					writer.setDefaultNamespace(config.namespace);
					writer.setSplitBySchema(config.splitBySchema);

					customTypeMappings = new LinkedHashMap<>();
					if (config.customTypeMappings != null) {
						for (Entry<String, String> kv : config.customTypeMappings.entrySet()) {
							Pattern p = Pattern.compile(kv.getKey());
							customTypeMappings.put(p, kv.getValue());
						}
					}

					customNameMappings = new LinkedHashMap<>();
					if (config.customNameMappings != null) {
						for (Entry<String, String> kv : config.customNameMappings.entrySet()) {
							Pattern p = Pattern.compile(kv.getKey());
							customNameMappings.put(p, kv.getValue());
						}
					}
					options = config.options;
					xp.setNestEnums(config.nestEnums);
					xp.setEnumOrderStart(0);
					xp.setTypeInEnums(config.typeInEnums);
					xp.setIncludeMessageDocs(config.includeMessageDocs);
					xp.setIncludeFieldDocs(config.includeFieldDocs);

				} catch (IOException e) {
					LOGGER.error("Unable to find config file " + configFile, e);
				} catch (YAMLException e) {
					LOGGER.error("Error parsing config file", e);

				}
			} else {
				i = 0;
				while (correct && i < args.length - 1) {
					if (args[i].startsWith("--filename=")) {
						param = args[i].split("=")[1];
						writer.setFilename(param);
					} else if (args[i].startsWith("--directory=")) {
						param = args[i].split("=")[1];
						writer.setDirectory(param);
					} else if (args[i].startsWith("--package=")) {
						param = args[i].split("=")[1];
						writer.setDefaultNamespace(param);
					} else if (args[i].startsWith("--splitBySchema=")) {
						param = args[i].split("=")[1];
						writer.setSplitBySchema("true".equals(param));
					} else if (args[i].startsWith("--customTypeMappings=")) {
						param = args[i].split("=")[1];
						customTypeMappings = new HashMap<Pattern, String>();
						for (String mapping : param.split(",")) {
							int colon = mapping.indexOf(':');
							if (colon > -1) {
								customTypeMappings.put(Pattern.compile(mapping.substring(0, colon)), mapping.substring(colon + 1));
							} else {
								usage(mapping + " is not a valid custom tyope mapping - use schematype:outputtype");
							}
						}
					} else if (args[i].startsWith("--customNameMappings=")) {
						param = args[i].split("=")[1];
						customNameMappings = new HashMap<Pattern, String>();
						for (String mapping : param.split(",")) {
							int colon = mapping.indexOf(':');
							if (colon > -1) {
								customNameMappings.put(Pattern.compile(mapping.substring(0, colon)), mapping.substring(colon + 1));
							} else {
								usage(mapping + " is not a valid custom name mapping - use originalname:newname");
							}
						}
					} else if (args[i].startsWith("--nestEnums=")) {
						param = args[i].split("=")[1];
						boolean nestEnums = Boolean.valueOf(param);
						xp.setNestEnums(nestEnums);
					} else if (args[i].startsWith("--typeInEnums=")) {
						xp.setTypeInEnums(Boolean.parseBoolean(args[i].split("=")[1]));
					} else if (args[i].startsWith("--includeMessageDocs=")) {
						xp.setIncludeMessageDocs(Boolean.parseBoolean(args[i].split("=")[1]));
					} else if (args[i].startsWith("--includeFieldDocs=")) {
						xp.setIncludeFieldDocs(Boolean.parseBoolean(args[i].split("=")[1]));
					} else {
						usage();
					}

					i = i + 1;
				}
			}

			if (customTypeMappings != null) {
				pbm.setCustomTypeMappings(customTypeMappings);
			}
			if (customNameMappings != null) {
				pbm.setCustomNameMappings(customNameMappings);
			}
			if (options != null) {
				pbm.setOptions(options);
			}

			if (correct) {
				try {

					xp.parse();
					LOGGER.info("Done");
				} catch (InvalidXSDException e) {
					LOGGER.error("Error converting xsd to proto: {}", e.getMessage());
				} catch (Exception e) {
					LOGGER.error("Error parsing xsd", e);
				}
			}
		}
	}
}
