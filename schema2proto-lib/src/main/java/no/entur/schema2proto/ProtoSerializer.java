package no.entur.schema2proto;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CaseFormat;
import com.squareup.wire.schema.*;
import com.squareup.wire.schema.internal.parser.OptionElement;
import com.squareup.wire.schema.internal.parser.OptionElement.Kind;

public class ProtoSerializer {

	public static final String UNDERSCORE = "_";
	private Schema2ProtoConfiguration configuration;

	private TypeAndNameMapper typeAndNameMapper;

	private static final Logger LOGGER = LoggerFactory.getLogger(SchemaParser.class);

	public static final String XML_NAMESPACE_PACKAGE = "www.w3.org.2001.XMLSchema";

	public ProtoSerializer(Schema2ProtoConfiguration configuration, TypeAndNameMapper marshaller) throws InvalidConfigrationException {
		this.configuration = configuration;
		this.typeAndNameMapper = marshaller;

		if (configuration.outputDirectory != null) {
			if (!configuration.outputDirectory.mkdirs() && !configuration.outputDirectory.exists()) {
				throw new InvalidConfigrationException("Could not create outputDirectory", null);
			}
		}
	}

	public void serialize(Map<String, ProtoFile> packageToProtoFileMap) throws InvalidXSDException, IOException {

		// Add options specified in config file
		addConfigurationSpecifiedOptions(packageToProtoFileMap);

		// Compute filenames based on package
		computeFilenames(packageToProtoFileMap);

		// Rewrite type information (replace xsd types with protobuf types/messages)
		translateTypes(packageToProtoFileMap);

		// Compute imports
		computeLocalImports(packageToProtoFileMap);

		// Add options specified in config file
		addConfigurationSpecifiedImports(packageToProtoFileMap);

		// Handle cases where identical field name comes from both attribute and element (but with different case)
		handleFieldNameCaseInsensitives(packageToProtoFileMap);

		// Rename fields
		translateFieldNames(packageToProtoFileMap);

		// Combine field.packageName and field.Typename to field.packageName.typeName
		moveFieldPackageNameToFieldTypeName(packageToProtoFileMap);

		// Adjust to naming standard
		underscoreFieldNames(packageToProtoFileMap);

		// Escape any field names identical to java reserved keywords
		escapeReservedJavaKeywords(packageToProtoFileMap);

		// Insert default value, prefix values and possibly escape values
		updateEnumValues(packageToProtoFileMap);

		// Run included linker to detect problems
		link(packageToProtoFileMap);

		// Collect all written proto files for later parsing
		List<File> writtenProtoFiles = new ArrayList<>();

		if (configuration.outputFilename != null) {
			if (packageToProtoFileMap.size() > 1) {

				LOGGER.error("Source schema contains multiple namespaces but specifies a single output file");
				throw new InvalidXSDException();
			} else {

				File outputFile = new File(configuration.outputDirectory, configuration.outputFilename);
				Writer writer = new FileWriter(outputFile);
				writer.write(packageToProtoFileMap.entrySet().iterator().next().getValue().toSchema());
				writer.close();

				writtenProtoFiles.add(outputFile);
			}
		} else {

			for (Entry<String, ProtoFile> entry : packageToProtoFileMap.entrySet()) {
				File outputFile = new File(configuration.outputDirectory, entry.getValue().location().getPath());
				Writer writer = new FileWriter(outputFile);
				writer.write(entry.getValue().toSchema());
				writer.close();

				writtenProtoFiles.add(outputFile);
			}
		}

		// Parse and verify written proto files
		parseWrittenFiles(writtenProtoFiles);

	}

	private void updateEnumValues(Map<String, ProtoFile> packageToProtoFileMap) {
		for (Entry<String, ProtoFile> protoFile : packageToProtoFileMap.entrySet()) {
			ProtoFile file = protoFile.getValue();
			List<Type> types = file.types();
			updateEnumValues(types);
		}
	}

	private void updateEnumValues(List<Type> types) {
		for (Type t : types) {
			if (t instanceof EnumType) {
				EnumType e = (EnumType) t;
				updateEnum(e);
			}
			updateEnumValues(t.nestedTypes());
		}
	}

	private void updateEnum(EnumType e) {
		// add UNSPECIFIED value first
		List<OptionElement> optionElementsUnspecified = new ArrayList<>();

		// Prefix with enum type name
		String enumValuePrefix = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, e.name()) + UNDERSCORE;
		for (EnumConstant ec : e.constants()) {
			String enumValue = escapeEnumValue(ec.getName());
			String escapedValue = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, enumValue);
			ec.updateName(enumValuePrefix + escapedValue);
		}
		EnumConstant unspecified = new EnumConstant(new Location("", "", 0, 0), enumValuePrefix + "UNSPECIFIED", 0, "Default",
				new Options(Options.ENUM_VALUE_OPTIONS, optionElementsUnspecified));
		e.constants().add(0, unspecified);
	}

	private String escapeEnumValue(String name) {
		switch (name) {
		case "+":
			return "plus";
		case "-":
			return "minus";
		default:
			return name;
		}

	}

	private void parseWrittenFiles(List<File> writtenProtoFiles) throws IOException {
		SchemaLoader schemaLoader = new SchemaLoader();

		try {
			schemaLoader.addSource(configuration.outputDirectory);

			for (File f : writtenProtoFiles) {
				schemaLoader.addProto(f.getAbsolutePath().substring(configuration.outputDirectory.getAbsolutePath().length() + 1));
			}
			Schema schema = schemaLoader.load();
		} catch (IOException e) {
			LOGGER.error("Parsing of written output failed, the proto files are not valid", e);
			throw e;
		}

	}

	private void link(Map<String, ProtoFile> packageToProtoFileMap) {
		Iterable<ProtoFile> iterable = getIterableFromIterator(packageToProtoFileMap.values().iterator());
		Linker linker = new Linker(iterable);
		try {
			linker.link();
		} catch (Exception e) {
			LOGGER.error("Linking failed, the proto file is not valid", e);
		}

	}

	public static <T> Iterable<T> getIterableFromIterator(Iterator<T> iterator) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return iterator;
			}
		};
	}

	private void computeFilenames(Map<String, ProtoFile> packageToProtoFileMap) {
		for (Entry<String, ProtoFile> protoFile : packageToProtoFileMap.entrySet()) {
			ProtoFile file = protoFile.getValue();
			String filename = protoFile.getKey().replaceAll("\\.", UNDERSCORE) + ".proto";
			Location loc = new Location("", filename, 0, 0);
			file.setLocation(loc);
		}
	}

	private void addConfigurationSpecifiedOptions(Map<String, ProtoFile> packageToProtoFileMap) {
		for (Entry<String, ProtoFile> protoFile : packageToProtoFileMap.entrySet()) {
			for (Entry<String, String> option : configuration.options.entrySet()) {

				OptionElement optionElement = new OptionElement(option.getKey(), Kind.STRING, option.getValue(), false);
				protoFile.getValue().options().add(optionElement);
			}
		}
	}

	private void addConfigurationSpecifiedImports(Map<String, ProtoFile> packageToProtoFileMap) {
		for (Entry<String, ProtoFile> protoFile : packageToProtoFileMap.entrySet()) {
			protoFile.getValue().imports().addAll(configuration.customImports);
		}
	}

	private void computeLocalImports(Map<String, ProtoFile> packageToProtoFileMap) {
		for (Entry<String, ProtoFile> entry : packageToProtoFileMap.entrySet()) {

			ProtoFile file = entry.getValue();
			SortedSet<String> imports = new TreeSet<>(file.imports());

			for (Type type : file.types()) {
				if (type instanceof MessageType) {
					MessageType mt = (MessageType) type;
					for (Field field : mt.fields()) {

						if (file.packageName() != null && file.packageName().equals(field.packageName())) {
							field.clearPackageName();
						}

						if (StringUtils.trimToNull(field.packageName()) != null && !field.packageName().equals(XML_NAMESPACE_PACKAGE)) {
							// Add import
							ProtoFile fileToImport = packageToProtoFileMap.get(field.packageName());
							if (fileToImport != null) {
								imports.add(fileToImport.location().getPath());
							} else {
								LOGGER.error("Tried to create import for field packageName " + field.packageName() + ", but no such protofile exist");
							}
						}
					}
				}
			}

			file.imports().clear();
			file.imports().addAll(imports);

		}
	}

	private void moveFieldPackageNameToFieldTypeName(Map<String, ProtoFile> packageToProtoFileMap) {
		for (Entry<String, ProtoFile> entry : packageToProtoFileMap.entrySet()) {

			ProtoFile file = entry.getValue();
			for (Type type : file.types()) {
				if (type instanceof MessageType) {
					MessageType mt = (MessageType) type;
					for (Field field : mt.fields()) {

						String fieldPackageName = StringUtils.trimToNull(field.packageName());
						if (fieldPackageName != null) {
							field.clearPackageName();
							field.updateElementType(fieldPackageName + "." + field.getElementType());
						}
					}
				}
			}
		}
	}

	private void translateTypes(Map<String, ProtoFile> packageToProtoFileMap) {
		for (Entry<String, ProtoFile> protoFile : packageToProtoFileMap.entrySet()) {
			for (Type type : protoFile.getValue().types()) {
				if (type instanceof MessageType) {
					MessageType mt = (MessageType) type;
					for (Field field : mt.fields()) {
						String fieldType = field.getElementType();
						String newFieldType = typeAndNameMapper.translateType(fieldType);
						field.updateElementType(newFieldType);
					}

					String messageName = mt.getName();
					String newMessageName = typeAndNameMapper.translateType(messageName);

					mt.updateName(newMessageName);
				}

			}

		}
	}

	private void translateFieldNames(Map<String, ProtoFile> packageToProtoFileMap) {
		for (Entry<String, ProtoFile> protoFile : packageToProtoFileMap.entrySet()) {
			for (Type type : protoFile.getValue().types()) {
				if (type instanceof MessageType) {
					MessageType mt = (MessageType) type;
					for (Field field : mt.fields()) {
						String fieldName = field.name();
						String newFieldName = typeAndNameMapper.translateName(fieldName);
						field.updateName(newFieldName);
					}
				}
			}
		}
	}

	private void handleFieldNameCaseInsensitives(Map<String, ProtoFile> packageToProtoFileMap) {
		for (Entry<String, ProtoFile> protoFile : packageToProtoFileMap.entrySet()) {
			for (Type type : protoFile.getValue().types()) {
				if (type instanceof MessageType) {
					MessageType mt = (MessageType) type;

					Set<String> fieldNamesUppercase = new HashSet<>();

					for (Field field : mt.fields()) {
						String fieldName = field.name();
						boolean existedBefore = fieldNamesUppercase.add(fieldName.toUpperCase());
						if (!existedBefore) {
							fieldName += UNDERSCORE;
							field.updateName(fieldName);
						}
					}
				}
			}
		}
	}

	private void underscoreFieldNames(Map<String, ProtoFile> packageToProtoFileMap) {
		for (Entry<String, ProtoFile> protoFile : packageToProtoFileMap.entrySet()) {
			for (Type type : protoFile.getValue().types()) {
				if (type instanceof MessageType) {
					MessageType mt = (MessageType) type;
					for (Field field : mt.fields()) {
						String fieldName = field.name();
						String newFieldName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldName);
						/*
						 * if(fieldName.startsWith(UNDERSCORE)) { newFieldName = UNDERSCORE+newFieldName; } if(fieldName.endsWith(UNDERSCORE)) { newFieldName +=
						 * UNDERSCORE; }
						 */

						field.updateName(newFieldName);
					}
				}
			}
		}
	}

	private void escapeReservedJavaKeywords(Map<String, ProtoFile> packageToProtoFileMap) {
		for (Entry<String, ProtoFile> protoFile : packageToProtoFileMap.entrySet()) {
			for (Type type : protoFile.getValue().types()) {
				if (type instanceof MessageType) {
					MessageType mt = (MessageType) type;
					for (Field field : mt.fields()) {
						String fieldName = field.name();
						String newFieldName = typeAndNameMapper.escapeFieldName(fieldName);
						field.updateName(newFieldName);
					}
				}
			}
		}
	}

}
