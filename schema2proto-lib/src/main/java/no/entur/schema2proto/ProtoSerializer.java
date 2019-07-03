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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.*;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CaseFormat;
import com.squareup.wire.schema.*;
import com.squareup.wire.schema.internal.parser.OptionElement;
import com.squareup.wire.schema.internal.parser.OptionElement.Kind;

public class ProtoSerializer {

	public static final String UNDERSCORE = "_";
	public static final String VALIDATION_PROTO_IMPORT = "validate/validate.proto";
	private Schema2ProtoConfiguration configuration;

	private TypeAndNameMapper typeAndFieldNameMapper;

	private Set<String> basicTypes = new HashSet<>();

	private static final Logger LOGGER = LoggerFactory.getLogger(SchemaParser.class);

	public ProtoSerializer(Schema2ProtoConfiguration configuration, TypeAndNameMapper marshaller) throws InvalidConfigurationException {
		this.configuration = configuration;
		this.typeAndFieldNameMapper = marshaller;
		basicTypes.addAll(TypeRegistry.getBasicTypes());

		if (configuration.outputDirectory != null) {
			if (!configuration.outputDirectory.mkdirs() && !configuration.outputDirectory.exists()) {
				throw new InvalidConfigurationException("Could not create outputDirectory", null);
			}
			LOGGER.info("Writing proto files to " + configuration.outputDirectory.getAbsolutePath());
		}
	}

	public void serialize(Map<String, ProtoFile> packageToProtoFileMap) throws InvalidXSDException, IOException {

		// Remove temporary generated name suffix
		replaceGeneratedSuffix(packageToProtoFileMap, SchemaParser.GENERATED_NAME_SUFFIX_UNIQUENESS, SchemaParser.TYPE_SUFFIX);

		// Uppercase message names
		uppercaseMessageNames(packageToProtoFileMap);

		// Add options specified in config file
		addConfigurationSpecifiedOptions(packageToProtoFileMap);

		// Compute filenames based on package
		computeFilenames(packageToProtoFileMap);

		// Rewrite type information (replace xsd types with protobuf types/messages)
		translateTypes(packageToProtoFileMap);

		// Compute imports
		computeLocalImports(packageToProtoFileMap);

		// Add imports specified in config file
		addConfigurationSpecifiedImports(packageToProtoFileMap);

		// Handle cases where identical field name comes from both attribute and element (but with different case)
		handleFieldNameCaseInsensitives(packageToProtoFileMap);

		// Rename fields
		translateFieldNames(packageToProtoFileMap);

		// Combine field.packageName and field.Typename to field.packageName.typeName
		moveFieldPackageNameToFieldTypeName(packageToProtoFileMap);

		// Add leading '.' to field.elementType if applicable
		addLeadingPeriodToElementType(packageToProtoFileMap);

		// Adjust to naming standard
		underscoreFieldNames(packageToProtoFileMap);

		// Escape any field names identical to java reserved keywords
		escapeReservedJavaKeywords(packageToProtoFileMap);

		// Insert default value, prefix values and possibly escape values
		updateEnumValues(packageToProtoFileMap);

		// Run included linker to detect problems
		// link(packageToProtoFileMap);

		// Collect all written proto files for later parsing
		List<File> writtenProtoFiles = new ArrayList<>();

		if (configuration.outputFilename != null) {
			if (packageToProtoFileMap.size() > 1) {

				LOGGER.error("Source schema contains multiple namespaces but specifies a single output file");
				throw new InvalidXSDException();
			} else {

				ProtoFile protoFile = packageToProtoFileMap.entrySet().iterator().next().getValue();
				File destFolder = createPackageFolderStructure(configuration.outputDirectory, protoFile.packageName());

				File outputFile = new File(destFolder, configuration.outputFilename.toLowerCase());
				Writer writer = new FileWriter(outputFile);
				writer.write(protoFile.toSchema());
				writer.close();

				writtenProtoFiles.add(outputFile);
			}
		} else {

			for (Entry<String, ProtoFile> entry : packageToProtoFileMap.entrySet()) {
				ProtoFile protoFile = entry.getValue();
				File destFolder = createPackageFolderStructure(configuration.outputDirectory, protoFile.packageName());
				File outputFile = new File(destFolder, protoFile.location().getPath().toLowerCase());
				Writer writer = new FileWriter(outputFile);
				writer.write(protoFile.toSchema());
				writer.close();

				writtenProtoFiles.add(outputFile);
			}
		}

		// Parse and verify written proto files
		parseWrittenFiles(writtenProtoFiles);

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

	private void replaceGeneratedSuffix(Map<String, ProtoFile> packageToProtoFileMap, String generatedRandomTypeSuffix, String newTypeSuffix) {
		for (Entry<String, ProtoFile> protoFile : packageToProtoFileMap.entrySet()) {
			replaceGeneratedSuffix(packageToProtoFileMap, generatedRandomTypeSuffix, newTypeSuffix, protoFile.getValue().types(),
					protoFile.getValue().packageName());
		}

	}

	private void replaceGeneratedSuffix(Map<String, ProtoFile> packageToProtoFileMap, String generatedRandomTypeSuffix, String newTypeSuffix, List<Type> types,
			String packageName) {
		Set<String> usedNames = findExistingTypeNamesInProtoFile(types);
		for (Type type : types) {
			// Recurse into nested types
			replaceGeneratedSuffix(packageToProtoFileMap, generatedRandomTypeSuffix, newTypeSuffix, type.nestedTypes(), packageName);

			if (type instanceof MessageType) {
				MessageType mt = (MessageType) type;

				String messageName = mt.getName();
				if (messageName.endsWith(generatedRandomTypeSuffix)) {
					String newMessageName = messageName.replace(generatedRandomTypeSuffix, newTypeSuffix);
					if (!usedNames.contains(newMessageName)) {
						mt.updateName(newMessageName);
						usedNames.add(newMessageName);
						updateTypeReferences(packageToProtoFileMap, packageName, messageName, newMessageName);
					} else {
						LOGGER.warn("Cannot rename message " + messageName + " to " + newMessageName + " as type already exist! Renaming ignored");
					}
				}
			} else if (type instanceof EnumType) {
				EnumType et = (EnumType) type;
				String messageName = et.name();
				if (messageName.endsWith(generatedRandomTypeSuffix)) {
					String newMessageName = messageName.replace(generatedRandomTypeSuffix, newTypeSuffix);
					if (!usedNames.contains(newMessageName)) {
						et.updateName(newMessageName);
						usedNames.add(newMessageName);
						updateTypeReferences(packageToProtoFileMap, packageName, messageName, newMessageName);
					} else {
						LOGGER.warn("Cannot rename enum " + messageName + " to " + newMessageName + " as type already exist! Renaming ignored");
					}
				}
			}
		}
	}

	private void uppercaseMessageNames(Map<String, ProtoFile> packageToProtoFileMap) {
		for (Entry<String, ProtoFile> protoFile : packageToProtoFileMap.entrySet()) {
			uppercaseMessageNames(packageToProtoFileMap, protoFile.getValue().types(), protoFile.getValue().packageName());
		}

	}

	private void uppercaseMessageNames(Map<String, ProtoFile> packageToProtoFileMap, List<Type> types, String packageName) {
		Set<String> usedNames = findExistingTypeNamesInProtoFile(types);
		for (Type type : types) {
			// Recurse into nested types
			uppercaseMessageNames(packageToProtoFileMap, type.nestedTypes(), packageName);

			if (type instanceof MessageType) {
				MessageType mt = (MessageType) type;

				String messageName = mt.getName();
				if (!Character.isUpperCase(messageName.charAt(0))) {
					String newMessageName = StringUtils.capitalize(messageName);
					if (!usedNames.contains(newMessageName)) {
						mt.updateName(newMessageName);
						usedNames.add(newMessageName);
						updateTypeReferences(packageToProtoFileMap, packageName, messageName, newMessageName);
					} else {
						LOGGER.warn("Cannot uppercase message " + messageName + " to " + newMessageName + " as type already exist! Renaming ignored");
					}
				}
			} else if (type instanceof EnumType) {
				EnumType et = (EnumType) type;
				String messageName = et.name();
				if (!Character.isUpperCase(messageName.charAt(0))) {
					String newMessageName = StringUtils.capitalize(messageName);
					if (!usedNames.contains(newMessageName)) {
						et.updateName(newMessageName);
						usedNames.add(newMessageName);
						updateTypeReferences(packageToProtoFileMap, packageName, messageName, newMessageName);
					} else {
						LOGGER.warn("Cannot uppercase enum " + messageName + " to " + newMessageName + " as type already exist! Renaming ignored");
					}
				}
			}
		}
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
			if (enumValue.equalsIgnoreCase("UNSPECIFIED")) {
				enumValue += "Value";
			}
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
			return name.replaceAll("-", "");
		}

	}

	private void parseWrittenFiles(List<File> writtenProtoFiles) throws IOException {
		SchemaLoader schemaLoader = new SchemaLoader();

		try {

			if (configuration.includeValidationRules) {
				schemaLoader.addProto("validate/validate.proto");
			}

			for (String importRootFolder : configuration.customImportLocations) {
				schemaLoader.addSource(new File(importRootFolder).toPath());
			}

			schemaLoader.addSource(configuration.outputDirectory);

			for (Path s : schemaLoader.sources()) {
				LOGGER.debug("Linking proto from path " + s);
			}
			for (String s : schemaLoader.protos()) {
				LOGGER.debug("Linking proto " + s);
			}

			Schema schema = schemaLoader.load();
		} catch (IOException e) {
			LOGGER.error("Parsing of written output failed, the proto files are not valid", e);
			throw e;
		}

	}

	/*
	 * private void link(Map<String, ProtoFile> packageToProtoFileMap) { Iterable<ProtoFile> iterable =
	 * getIterableFromIterator(packageToProtoFileMap.values().iterator()); Linker linker = new Linker(iterable); try { linker.link(); } catch (Exception e) {
	 * LOGGER.error("Linking failed, the proto file is not valid", e); }
	 *
	 * }
	 */
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
			for (Entry<String, Object> option : configuration.options.entrySet()) {

				Kind kind = null;
				if (option.getValue() instanceof Boolean) {
					kind = Kind.BOOLEAN;
				} else if (option.getValue() instanceof Number) {
					kind = Kind.NUMBER;
				} else {
					kind = Kind.STRING;
				}

				OptionElement optionElement = new OptionElement(option.getKey(), kind, option.getValue(), false);
				protoFile.getValue().options().add(optionElement);
			}
		}
	}

	private void addConfigurationSpecifiedImports(Map<String, ProtoFile> packageToProtoFileMap) {
		for (Entry<String, ProtoFile> protoFile : packageToProtoFileMap.entrySet()) {
			protoFile.getValue().imports().addAll(configuration.customImports);

			if (configuration.includeValidationRules) {
				protoFile.getValue().imports().add(VALIDATION_PROTO_IMPORT);
			}
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
						String packageName = StringUtils.trimToNull(field.packageName());
						if (file.packageName() != null && file.packageName().equals(packageName)) {
							field.clearPackageName();
						} else if (packageName != null) {
							// Add import
							ProtoFile fileToImport = packageToProtoFileMap.get(packageName);
							if (fileToImport != null) {
								imports.add(getPathFromPackageName(packageName) + "/" + fileToImport.location().getPath());
							} else {
								LOGGER.error("Tried to create import for field packageName " + packageName + ", but no such protofile exist");
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

	/*
	 * Adds leading '.' to field.elementType when needed. Ref.: https://developers.google.com/protocol-buffers/docs/proto3#packages-and-name-resolution
	 */
	private void addLeadingPeriodToElementType(Map<String, ProtoFile> packageToProtoFileMap) {
		for (Entry<String, ProtoFile> entry : packageToProtoFileMap.entrySet()) {

			ProtoFile file = entry.getValue();
			for (Type type : file.types()) {
				if (type instanceof MessageType) {
					MessageType mt = (MessageType) type;
					for (Field field : mt.fields()) {

						String fieldElementType = StringUtils.trimToNull(field.getElementType());
						if (fieldElementType != null) {
							if (fieldElementType.contains(".")) {
								for (String pkg : packageToProtoFileMap.keySet()) {
									if (!fieldElementType.equals(pkg)) {
										String rootFieldElementType = fieldElementType.split("\\.")[0];
										if (pkg.contains("." + rootFieldElementType + ".")) {
											// elementType should only be prepended when root-package of elementType matches a non-root package
											field.updateElementType("." + fieldElementType);
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	private void translateTypes(Map<String, ProtoFile> packageToProtoFileMap) {
		for (Entry<String, ProtoFile> protoFile : packageToProtoFileMap.entrySet()) {
			translateTypes(packageToProtoFileMap, protoFile.getValue().types(), protoFile.getValue().packageName());
		}
	}

	private void translateTypes(Map<String, ProtoFile> packageToProtoFileMap, List<Type> types, String packageName) {
		if (types.size() > 0) {
			Set<String> usedNames = findExistingTypeNamesInProtoFile(types);
			for (Type type : types) {
				if (type instanceof MessageType) {
					MessageType mt = (MessageType) type;

					translateTypes(packageToProtoFileMap, type.nestedTypes(), packageName);

					String messageName = mt.getName();
					String newMessageName = typeAndFieldNameMapper.translateType(messageName);
					if (newMessageName.contains("_"))
						LOGGER.info(messageName + "  <--->  " + newMessageName);

					if (!messageName.equals(newMessageName)) {
						if (!usedNames.contains(newMessageName)) {
							mt.updateName(newMessageName);
							usedNames.add(newMessageName);
							updateTypeReferences(packageToProtoFileMap, packageName, messageName, newMessageName);
						} else {
							LOGGER.error("Cannot rename message " + messageName + " to " + newMessageName + " as type already exist! Renaming ignored");
						}

					}
					for (Field field : mt.fields()) {
						// Translate basic types as well
						if (field.packageName() == null && basicTypes.contains(field.getElementType())) {
							String newFieldType = typeAndFieldNameMapper.translateType(field.getElementType());
							if (!newFieldType.equals(field.getElementType())) {
								LOGGER.debug("Replacing basicType " + field.getElementType() + " with " + newFieldType);
								field.updateElementType(newFieldType);
							}
						}

					}
				} else if (type instanceof EnumType) {
					EnumType et = (EnumType) type;
					String messageName = et.name();
					String newMessageName = typeAndFieldNameMapper.translateType(messageName);

					if (!messageName.equals(newMessageName)) {
						if (!usedNames.contains(newMessageName)) {
							et.updateName(newMessageName);
							usedNames.add(newMessageName);
							updateTypeReferences(packageToProtoFileMap, packageName, messageName, newMessageName);
						} else {
							LOGGER.error("Cannot rename enum " + messageName + " to " + newMessageName + " as type already exist! Renaming ignored");
						}
					}
				}
			}
		}
	}

	private void updateTypeReferences(Map<String, ProtoFile> packageToProtoFileMap, String packageNameOfType, String oldName, String newName) {
		for (Entry<String, ProtoFile> protoFile : packageToProtoFileMap.entrySet()) {
			updateTypeReferences(packageNameOfType, oldName, newName, protoFile.getValue().types());
		}

	}

	private void updateTypeReferences(String packageNameOfType, String oldName, String newName, List<Type> types) {
		for (Type type : types) {
			updateTypeReferences(packageNameOfType, oldName, newName, type.nestedTypes());

			if (type instanceof MessageType) {
				MessageType mt = (MessageType) type;

				for (Field field : mt.fields()) {
					if (samePackage(field.packageName(), packageNameOfType)) {
						String fieldType = field.getElementType();
						if (fieldType.equals(oldName)) {
							field.updateElementType(newName);
							LOGGER.debug("Updating field " + oldName + " in type " + mt.getName() + " to " + newName);
						}
					}
				}
			}
		}
	}

	private boolean samePackage(String packageNameOfFile, String packageNameOfReferencedTypeInField) {
		if (packageNameOfFile == null && packageNameOfReferencedTypeInField == null) {
			return true;
		} else if (packageNameOfFile == null) {
			return false;
		} else if (packageNameOfReferencedTypeInField == null) {
			return false;
		} else {
			return packageNameOfFile.equals(packageNameOfReferencedTypeInField);
		}
	}

	private Set<String> findExistingTypeNamesInProtoFile(List<Type> types) {
		Set<String> existingTypeNames = new HashSet<>();
		for (Type t : types) {
			if (t instanceof MessageType) {
				existingTypeNames.add(((MessageType) t).getName());
			} else if (t instanceof EnumType) {
				existingTypeNames.add(((EnumType) t).name());
			}
		}

		return existingTypeNames;
	}

	private void translateFieldNames(Map<String, ProtoFile> packageToProtoFileMap) {
		for (Entry<String, ProtoFile> protoFile : packageToProtoFileMap.entrySet()) {
			for (Type type : protoFile.getValue().types()) {
				if (type instanceof MessageType) {
					MessageType mt = (MessageType) type;
					for (Field field : mt.fields()) {
						String fieldName = field.name();
						String newFieldName = typeAndFieldNameMapper.translateFieldName(fieldName);
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
							fieldName = fieldName + UNDERSCORE + "v";
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
						boolean startsWithUnderscore = fieldName.startsWith(UNDERSCORE);
						boolean endsWithUnderscore = fieldName.endsWith(UNDERSCORE);

						String strippedFieldName = StringUtils.removeEnd(StringUtils.removeStart(fieldName, UNDERSCORE), UNDERSCORE);

						String newFieldName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, strippedFieldName);

						if (endsWithUnderscore) {
							newFieldName += "u"; // Trailing underscore not accepted by protoc for java
						}

						if (startsWithUnderscore) {
							newFieldName = UNDERSCORE + newFieldName;
						}

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
						String newFieldName = typeAndFieldNameMapper.escapeFieldName(fieldName);
						field.updateName(newFieldName);
					}
				}
			}
		}
	}

}
