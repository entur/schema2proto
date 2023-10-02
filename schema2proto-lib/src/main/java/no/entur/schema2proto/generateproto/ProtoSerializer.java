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

import static no.entur.schema2proto.generateproto.GoPackageNameHelper.packageNameToGoPackageName;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IllegalFormatException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.CaseFormat;
import com.squareup.wire.schema.EnumConstant;
import com.squareup.wire.schema.EnumType;
import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.OneOf;
import com.squareup.wire.schema.Options;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.SchemaLoader;
import com.squareup.wire.schema.Type;
import com.squareup.wire.schema.internal.parser.OptionElement;
import com.squareup.wire.schema.internal.parser.OptionElement.Kind;
import com.sun.xml.xsom.XSComplexType;
import com.sun.xml.xsom.XSComponent;

import no.entur.schema2proto.InvalidConfigurationException;
import no.entur.schema2proto.compatibility.BackwardsCompatibilityCheckException;
import no.entur.schema2proto.compatibility.ProtolockBackwardsCompatibilityChecker;

public class ProtoSerializer {

	private static final String VALIDATION_PROTO_IMPORT = "validate/validate.proto";
	private static final String XSDOPTIONS_PROTO_IMPORT = "xsd/xsd.proto";
	private static final String UNDERSCORE = "_";
	private static final String DASH = "-";
	private static final String[] PACKABLE_SCALAR_TYPES = new String[] { "int32", "int64", "uint32", "uint64", "sint32", "sint64", "bool" };
	private static final Set<String> PACKABLE_SCALAR_TYPES_SET = new HashSet<>(Arrays.asList(PACKABLE_SCALAR_TYPES));

	private Schema2ProtoConfiguration configuration;

	private TypeAndNameMapper typeAndFieldNameMapper;

	private Set<String> basicTypes = new HashSet<>();

	private Map<String, String> customTypeImportToProtoFile = new HashMap<>();

	private static final Logger LOGGER = LoggerFactory.getLogger(ProtoSerializer.class);

	private ProtolockBackwardsCompatibilityChecker backwardsCompatibilityChecker;

	public ProtoSerializer(Schema2ProtoConfiguration configuration, TypeAndNameMapper marshaller) throws InvalidConfigurationException {
		this.configuration = configuration;
		this.typeAndFieldNameMapper = marshaller;
		basicTypes.addAll(TypeRegistry.getBasicTypes());

		if (configuration.outputDirectory != null) {
			if (!configuration.outputDirectory.mkdirs() && !configuration.outputDirectory.exists()) {
				throw new InvalidConfigurationException("Could not create outputDirectory", null);
			}
			LOGGER.info("Writing proto files to {}", configuration.outputDirectory.getAbsolutePath());
		}

		backwardsCompatibilityChecker = new ProtolockBackwardsCompatibilityChecker();
		if (configuration.protoLockFile != null) {
			try {
				backwardsCompatibilityChecker.init(configuration.protoLockFile);
			} catch (FileNotFoundException e) {
				throw new InvalidConfigurationException("Could not find proto.lock file, check configuration");
			}
		}

	}

	public void serialize(Map<String, ProtoFile> packageToProtoFileMap, List<LocalType> localTypes) throws InvalidXSDException, IOException {

		// Remove temporary generated name suffix
		replaceGeneratedTypePlaceholder(packageToProtoFileMap, SchemaParser.GENERATED_NAME_PLACEHOLDER, SchemaParser.TYPE_SUFFIX);

		// Sort types in files
		sortTypesInProtofile(packageToProtoFileMap);

		// Reorganize reused embedded types into global types referenced from the
		moveReusedLocalTypesToGlobal(packageToProtoFileMap, localTypes);

		// Remove unwanted fields
		removeUnwantedFields(packageToProtoFileMap);

		// Uppercase message names
		uppercaseMessageNames(packageToProtoFileMap);

		// Add options specified in config file
		addConfigurationSpecifiedOptions(packageToProtoFileMap);

		// Compute filenames based on package
		computeFilenames(packageToProtoFileMap);

		// Rewrite type information (replace xsd types with protobuf types/messages)
		translateTypes(packageToProtoFileMap);

		// Replace types with other. Opposed to translateTypes this method does not change MessageTypes, but only references from Fields
		replaceTypes(packageToProtoFileMap);

		// Compute imports
		computeLocalImports(packageToProtoFileMap);

		// Add imports specified in config file - IF they are actually in use
		addConfigurationSpecifiedImports(packageToProtoFileMap);

		// Find out if a file recursively imports itself
		resolveRecursiveImports(packageToProtoFileMap);

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

		// Add packed=true option to repeated enum or number fields
		addPackedOptionToRepeatedFields(packageToProtoFileMap, true);

		// Add go_package options to all files
		if (configuration.includeGoPackageOptions) {
			includeGoPackageNameOptions(packageToProtoFileMap);
		}

		// Try to resolve some backward incompatibilities based on protolock
		boolean possibleIncompatibilitiesDetected = false;
		if (configuration.protoLockFile != null) {
			possibleIncompatibilitiesDetected = resolveBackwardIncompatibilities(packageToProtoFileMap);
		}

		// Sort fields by tag/id
		sortFieldsByTag(packageToProtoFileMap);

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

				File outputFile = new File(destFolder, configuration.outputFilename.lowercase());
				try (Writer writer = new FileWriter(outputFile)) {
					writer.write(protoFile.toSchema());

				}
				writtenProtoFiles.add(outputFile);
			}
		} else {

			for (Entry<String, ProtoFile> entry : packageToProtoFileMap.entrySet()) {
				ProtoFile protoFile = entry.getValue();
				File destFolder = createPackageFolderStructure(configuration.outputDirectory, protoFile.packageName());
				File outputFile = new File(destFolder, protoFile.location().getPath().lowercase());

				try (Writer writer = new FileWriter(outputFile)) {
					writer.write(protoFile.toSchema());

				}
				writtenProtoFiles.add(outputFile);
			}
		}

		// Parse and verify written proto files
		parseWrittenFiles();

		if (possibleIncompatibilitiesDetected && configuration.failIfRemovedFields) {
			throw new BackwardsCompatibilityCheckException(
					"Possible backwards incompatibility detected. See previous log messages. Re-run with option failIfRemovedFields=false if this is ok");
		}

	}

	private void sortFieldsByTag(Map<String, ProtoFile> packageToProtoFileMap) {
		for (ProtoFile file : packageToProtoFileMap.values()) {
			messageTypes(file.types()).forEach(mt -> {
				mt.nestedTypes().stream().filter(MessageType.class::isInstance).forEach(z -> sortFieldsByTag((MessageType) z));
				sortFieldsByTag(mt);
			});
		}
	}

	private void sortFieldsByTag(MessageType mt) {
		Collections.sort(mt.fields(), Comparator.comparingInt(Field::tag));
		mt.oneOfs().forEach(oneOf -> Collections.sort(oneOf.fields(), Comparator.comparingInt(Field::tag)));
	}

	private boolean resolveBackwardIncompatibilities(Map<String, ProtoFile> packageToProtoFileMap) {
		LOGGER.debug("Checking for backward incompatible changes");

		AtomicBoolean possibleIncompatibilitiesDetected = new AtomicBoolean(false);

		for (ProtoFile file : packageToProtoFileMap.values()) {
			if (backwardsCompatibilityChecker.resolveBackwardIncompatibilities(file)) {
				possibleIncompatibilitiesDetected.set(true);
			}
		}

		LOGGER.debug("Checking for backward incompatible changes - completed");
		return possibleIncompatibilitiesDetected.get();
	}

	private void sortTypesInProtofile(Map<String, ProtoFile> packageToProtoFileMap) {
		packageToProtoFileMap.values().forEach(e -> e.types().sort(Comparator.comparing(x -> x.type().simpleName())));

	}

	private void addPackedOptionToRepeatedFields(Map<String, ProtoFile> packageToProtoFileMap, boolean b) {
		for (ProtoFile file : packageToProtoFileMap.values()) {
			messageTypes(file.types()).forEach(mt -> {
				messageTypes(mt.nestedTypes()).forEach(e -> addPackedOptionToRepeatedFields(packageToProtoFileMap, file, e, b));
				addPackedOptionToRepeatedFields(packageToProtoFileMap, file, mt, b);
			});
		}
	}

	private void addPackedOptionToRepeatedFields(Map<String, ProtoFile> packageToProtoFileMap, ProtoFile protoFile, MessageType mt, boolean packed) {
		mt.fields()
				.stream()
				.filter(e -> e.label() == Field.Label.REPEATED)
				.filter(e -> isExposedToPackedBug(packageToProtoFileMap, protoFile, e))
				.forEach(e -> addPackedOption(e, packed));
	}

	private void addPackedOption(Field f, boolean packed) {
		OptionElement packedOptionElement = new OptionElement("packed", Kind.BOOLEAN, packed, false);
		f.options().getOptionElements().add(packedOptionElement);
	}

	private boolean isExposedToPackedBug(Map<String, ProtoFile> packageToProtoFileMap, ProtoFile protoFile, Field elementType) {
		return PACKABLE_SCALAR_TYPES_SET.contains(elementType.getElementType()) || isEnum(elementType, protoFile, packageToProtoFileMap);
	}

	private boolean isEnum(Field elementType, ProtoFile protoFile, Map<String, ProtoFile> packageToProtoFileMap) {
		String packageName = elementType.packageName();
		if (packageName != null) {
			protoFile = packageToProtoFileMap.get(packageName);
		}
		return protoFile.types().stream().filter(EnumType.class::isInstance).anyMatch(e -> ((EnumType) e).name().equals(elementType.getElementType()));
	}

	private void removeUnwantedFields(Map<String, ProtoFile> packageToProtoFileMap) {
		for (ProtoFile file : packageToProtoFileMap.values()) {
			messageTypes(file.types()).forEach(mt -> removeUnwantedFields(file, "", mt));
		}
	}

	private void removeUnwantedFields(ProtoFile file, final String outerMessagePath, MessageType mt) {
		// Recursive nested type removal
		StringBuilder nestedPathBuilder = new StringBuilder();
		if (!outerMessagePath.equals("")) {
			nestedPathBuilder.append(outerMessagePath);
			nestedPathBuilder.append(".");
		}
		nestedPathBuilder.append(mt.getName());

		final String nestedPath = nestedPathBuilder.toString();

		messageTypes(mt.nestedTypes()).forEach(nestedType -> removeUnwantedFields(file, nestedPath, nestedType));

		String path = "";
		if (!outerMessagePath.equals("")) {
			path = outerMessagePath + ".";
		}

		List<Field> fieldsToRemove = removeUnwantedFields(file.packageName(), path + mt.getName(), mt.fields());
		for (Field f : fieldsToRemove) {
			mt.removeDeclaredField(f);
			String documentation = StringUtils.trimToEmpty(mt.documentation());
			documentation += " NOTE: Removed field " + f;
			mt.updateDocumentation(documentation);
		}

		List<OneOf> oneOfsToRemove = new ArrayList<>();

		for (OneOf oneOf : mt.oneOfs()) {

			List<Field> oneOfFieldsToRemove = removeUnwantedFields(file.packageName(), path + mt.getName(), oneOf.fields());
			for (Field f : oneOfFieldsToRemove) {
				oneOf.fields().remove(f);

				String documentation = StringUtils.trimToEmpty(mt.documentation());
				documentation += " NOTE: Removed field " + f;
				oneOf.updateDocumentation(documentation);
			}

			if (oneOf.fields().isEmpty()) {
				// remove oneof
				oneOfsToRemove.add(oneOf);
			}
		}

		// Remove empty oneOfs
		for (OneOf oneOfToRemove : oneOfsToRemove) {

			mt.removeOneOf(oneOfToRemove);
			String documentation = StringUtils.trimToEmpty(mt.documentation());
			documentation += " NOTE: Removed empty oneOf " + oneOfToRemove;
			mt.updateDocumentation(documentation);
		}
	}

	private List<Field> removeUnwantedFields(String packageName, String messageName, List<Field> fields) {

		List<Field> fieldsToRemove = new ArrayList<>();

		for (Field field : fields) {
			if (typeAndFieldNameMapper.ignoreOutputField(packageName, messageName, field.name())) {
				fieldsToRemove.add(field);
			}
		}

		return fieldsToRemove;
	}

	private File createPackageFolderStructure(File outputDirectory, String packageName) {

		String folderSubstructure = getPathFromPackageName(packageName);
		File dstFolder = new File(outputDirectory, folderSubstructure);
		dstFolder.mkdirs();

		return dstFolder;

	}

	@NotNull
	private String getPathFromPackageNameAndType(String packageName, Type type) {

		String protoFileName = customTypeImportToProtoFile.get(buildFullyQualifiedTypeName(packageName, type));
		if (protoFileName != null) {
			return protoFileName;
		} else {
			return getPathFromPackageName(packageName);
		}
	}

	@NotNull
	private String getPathFromPackageName(String packageName) {
		return packageName.replace('.', '/');
	}

	private String buildFullyQualifiedTypeName(String packageName, Type type) {
		return packageName + "." + type.type().simpleName();
	}

	// Loads imported file and reads all qualified types
	@NotNull
	private List<String> getFullyQualifiedTypes(String pathName) {
		List<String> typeNames = new ArrayList<>();
		try {
			for (String customImportLocation : configuration.customImportLocations) {
				File path = new File(customImportLocation);
				if (!path.exists()) {
					throw new ConversionException("Custom import location " + customImportLocation + " does not exist");
				} else {
					File fileToImport = new File(path, pathName);
					if (fileToImport.exists()) {
						SchemaLoader schemaLoader = new SchemaLoader();
						schemaLoader.addSource(path);
						schemaLoader.addProto(pathName);
						Schema schema = schemaLoader.load();
						ProtoFile customImportFile = schema.protoFile(pathName);

						for (Type type : customImportFile.types()) {
							String qualifiedName = buildFullyQualifiedTypeName(customImportFile.packageName(), type);
							typeNames.add(qualifiedName);

							customTypeImportToProtoFile.put(qualifiedName, pathName);
						}
						break;
					}
				}
			}
			if (typeNames.isEmpty()) {
				throw new ConversionException("Custom import file " + pathName + " not found, looked in folders "
						+ ReflectionToStringBuilder.toString(configuration.customImportLocations.toArray()));
			}

		} catch (IOException e) {
			throw new ConversionException("Could not get packageName from custom imported file " + pathName, e);
		}

		return typeNames;

	}

	private void replaceGeneratedTypePlaceholder(Map<String, ProtoFile> packageToProtoFileMap, String generatedRandomTypeSuffix, String newTypeSuffix) {

		for (Entry<String, ProtoFile> protoFile : packageToProtoFileMap.entrySet()) {
			replaceGeneratedTypePlaceholder(packageToProtoFileMap, generatedRandomTypeSuffix, newTypeSuffix, protoFile.getValue().types(),
					protoFile.getValue().packageName());
		}
	}

	private void replaceGeneratedTypePlaceholder(Map<String, ProtoFile> packageToProtoFileMap, String generatedRandomTypePlaceholder, String newTypeSuffix,
			List<Type> types, String packageName) {
		Set<String> usedNames = findExistingTypeNamesInProtoFile(types);
		for (Type type : types) {
			// Recurse into nested types
			replaceGeneratedTypePlaceholder(packageToProtoFileMap, generatedRandomTypePlaceholder, newTypeSuffix, type.nestedTypes(), packageName);

			if (type instanceof MessageType) {
				MessageType mt = (MessageType) type;
				replaceGeneratedTypePlaceholder(packageToProtoFileMap, generatedRandomTypePlaceholder, newTypeSuffix, packageName, usedNames, mt);

			} else if (type instanceof EnumType) {
				EnumType et = (EnumType) type;
				String messageName = et.name();
				if (messageName.contains(generatedRandomTypePlaceholder)) {
					String newMessageName = messageName.replaceAll(generatedRandomTypePlaceholder, newTypeSuffix);
					if (!usedNames.contains(newMessageName)) {
						et.updateName(newMessageName);
						usedNames.add(newMessageName);
						updateTypeReferences(packageToProtoFileMap, packageName, messageName, newMessageName);
					} else {
						LOGGER.warn("Cannot rename enum {} to {} as type already exist! Renaming ignored", messageName, newMessageName);
					}
				}
			}
		}
	}

	private void replaceGeneratedTypePlaceholder(Map<String, ProtoFile> packageToProtoFileMap, String generatedRandomTypePlaceholder, String newTypeSuffix,
			String packageName, Set<String> usedNames, MessageType mt) {
		String messageName = mt.getName();
		if (messageName.contains(generatedRandomTypePlaceholder)) {
			String newMessageName = messageName.replaceAll(generatedRandomTypePlaceholder, newTypeSuffix);
			if (!usedNames.contains(newMessageName)) {
				mt.updateName(newMessageName);
				usedNames.add(newMessageName);
				updateTypeReferences(packageToProtoFileMap, packageName, messageName, newMessageName);
			} else {
				LOGGER.warn("Cannot rename message {} to {} as type already exist! Renaming ignored", messageName, newMessageName);
			}
		}
	}

	private void uppercaseMessageNames(Map<String, ProtoFile> packageToProtoFileMap) {
		for (ProtoFile file : packageToProtoFileMap.values()) {
			uppercaseMessageNames(packageToProtoFileMap, file.types(), file.packageName());
		}

	}

	private void uppercaseMessageNames(Map<String, ProtoFile> packageToProtoFileMap, List<Type> types, String packageName) {
		Set<String> usedNames = findExistingTypeNamesInProtoFile(types);
		for (Type type : types) {
			// Recurse into nested types
			uppercaseMessageNames(packageToProtoFileMap, type.nestedTypes(), packageName);

			if (type instanceof MessageType) {
				MessageType mt = (MessageType) type;

				uppercaseMessageNames(packageToProtoFileMap, packageName, usedNames, mt);
			} else if (type instanceof EnumType) {
				EnumType et = (EnumType) type;
				String messageName = et.name();

				String newMessageName = messageName;
				try {
					newMessageName = getCaseFormatName(messageName).to(CaseFormat.UPPER_CAMEL, messageName);
				} catch (IllegalFormatException e) {
					// Ignore
				}

				if (!newMessageName.equals(messageName)) {
					if (!usedNames.contains(newMessageName)) {
						et.updateName(newMessageName);
						usedNames.add(newMessageName);
						updateTypeReferences(packageToProtoFileMap, packageName, messageName, newMessageName);
					} else {
						LOGGER.warn("Cannot uppercase enum {} to {} as type already exist! Renaming ignored", messageName, newMessageName);
					}
				}
			}
		}
	}

	private CaseFormat getCaseFormatName(String s) throws IllegalFormatException {
		if (s.contains("_")) {
			if (Character.isLowerCase(s.charAt(0))) {
				return CaseFormat.LOWER_UNDERSCORE;
			} else {
				return CaseFormat.UPPER_UNDERSCORE;
			}
		} else if (s.contains("-")) {
			if (s.lowercase().equals(s)) {
				return CaseFormat.LOWER_HYPHEN;
			}
		} else {
			if (Character.isLowerCase(s.charAt(0))) {
				if (s.matches("([a-z]+[A-Z0-9]+\\w+)+")) {
					return CaseFormat.LOWER_CAMEL;
				} else if (s.matches("[a-z]+")) {
					return CaseFormat.LOWER_UNDERSCORE;
				}
			} else {
				if (s.matches("([A-Z]+[a-z0-9]+\\w+)+")) {
					return CaseFormat.UPPER_CAMEL;
				} else if (s.matches("[A-Z]+")) {
					return CaseFormat.UPPER_UNDERSCORE;
				}
			}
		}

		throw new IllegalArgumentException(String.format("Couldn't find the case format of the given string '%s'", s));
	}

	private void uppercaseMessageNames(Map<String, ProtoFile> packageToProtoFileMap, String packageName, Set<String> usedNames, MessageType mt) {
		String messageName = mt.getName();
		if (!Character.isUpperCase(messageName.charAt(0))) {
			String newMessageName = StringUtils.capitalize(messageName);
			if (!usedNames.contains(newMessageName)) {
				mt.updateName(newMessageName);
				usedNames.add(newMessageName);
				updateTypeReferences(packageToProtoFileMap, packageName, messageName, newMessageName);
			} else {
				LOGGER.warn("Cannot uppercase message {} to {} as type already exist! Renaming ignored", messageName, newMessageName);
			}
		}
	}

	private void updateEnumValues(Map<String, ProtoFile> packageToProtoFileMap) {
		for (ProtoFile file : packageToProtoFileMap.values()) {
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
				enumValue = "UNSPECIFIED_ENUM_VALUE"; // Handle collision with UNSPECIFIED special value according to Google style guide
			}
			ec.updateName(enumValuePrefix + enumValue);
		}
		EnumConstant unspecified = new EnumConstant(new Location("", "", 0, 0), enumValuePrefix + "UNSPECIFIED", 0, "Default",
				new Options(Options.ENUM_VALUE_OPTIONS, optionElementsUnspecified));
		e.constants().add(0, unspecified);

	}

	private String escapeEnumValue(String name) {
		if (name.equals("")) {
			return name;
		}

		try {
			switch (name) {
			case "+":
				return "PLUS";
			case "-":
				return "MINUS";
			default: {
				// Replace any non standard characters with space
				String transformationBasis = name.replaceAll("[^a-zA-Z0-9]+", " ").trim();
				// Split by whitespace first
				String[] parts = transformationBasis.split(" ");
				List<String> modifiedParts = new ArrayList<>();
				for (String part : parts) {
					modifiedParts.addAll(Arrays.asList(StringUtils.splitByCharacterTypeCamelCase(part)));
				}

				// Join all parts by underscore
				transformationBasis = StringUtils.join(modifiedParts, "_");

				// Uppercase everything
				return transformationBasis.toUpperCase();
			}
			}
		} catch (Exception e) {
			LOGGER.warn("Error escaping enum value {}, using original. May break proto file", name, e);
			return name;
		}

	}

	private void parseWrittenFiles() throws IOException {
		SchemaLoader schemaLoader = new SchemaLoader();

		try {
			if (configuration.includeValidationRules) {
				schemaLoader.addProto(VALIDATION_PROTO_IMPORT);
			}
			if (configuration.includeXsdOptions) {
				schemaLoader.addProto(XSDOPTIONS_PROTO_IMPORT);
			}

			for (String importRootFolder : configuration.customImportLocations) {
				schemaLoader.addSource(new File(importRootFolder).toPath());
			}

			schemaLoader.addSource(configuration.outputDirectory);

			for (Path s : schemaLoader.sources()) {
				LOGGER.debug("Linking proto from path {}", s);
			}
			for (String s : schemaLoader.protos()) {
				LOGGER.debug("Linking proto {}", s);
			}

			schemaLoader.load();
		} catch (IOException e) {
			throw new ConversionException("Parsing of written output failed, the proto files are not valid", e);
		}

	}

	/*
	 * private void link(Map<String, ProtoFile> packageToProtoFileMap) { Iterable<ProtoFile> iterable =
	 * getIterableFromIterator(packageToProtoFileMap.values().iterator()); Linker linker = new Linker(iterable); try { linker.link(); } catch (Exception e) {
	 * LOGGER.error("Linking failed, the proto file is not valid", e); }
	 *
	 * }
	 *
	 * public static <T> Iterable<T> getIterableFromIterator(Iterator<T> iterator) { return new Iterable<T>() {
	 *
	 * @Override public Iterator<T> iterator() { return iterator; } }; }
	 */
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

	private void includeGoPackageNameOptions(Map<String, ProtoFile> packageToProtoFileMap) {
		for (ProtoFile protoFile : packageToProtoFileMap.values()) {
			String optionName = "go_package";
			boolean alreadySet = protoFile.options().getOptionElements().stream().anyMatch(existingOption -> optionName.equals(existingOption.getName()));
			if (!alreadySet) {
				String goPackageName = packageNameToGoPackageName(configuration.goPackageSourcePrefix, protoFile.packageName());
				OptionElement optionElement = new OptionElement(optionName, OptionElement.Kind.STRING, goPackageName, false);
				protoFile.options().add(optionElement);
			}
		}
	}

	private void resolveRecursiveImports(Map<String, ProtoFile> packageToProtoFileMap) {

		Map<String, List<String>> imports = new HashMap<>();
		for (ProtoFile file : packageToProtoFileMap.values()) {

			List<String> fileImports = new ArrayList<>();
			fileImports.addAll(file.imports());
			for (int i = 0; i < fileImports.size(); i++) {
				// Removing path-info from fileimport
				fileImports.set(i, fileImports.get(i).substring(fileImports.get(i).lastIndexOf('/') + 1));
			}
			imports.put(file.toString(), fileImports);
		}

		for (Entry<String, ProtoFile> protoFileEntry : packageToProtoFileMap.entrySet()) {
			ProtoFile protoFile = protoFileEntry.getValue();
			String filename = protoFile.location().getPath();
			if (hasRecursiveImports(imports, filename, filename)) {
				LOGGER.error("File {} recursively imports itself.", filename);
				// TODO: Extract affected types to a separate, common ProtoFile
			}
		}

	}

	/**
	 * Checks imports recursively to resolve import-loops. E.g. A imports B, B imports C, C imports A
	 *
	 * @param imports
	 * @param rootFilename
	 * @param filename
	 * @return
	 */
	private boolean hasRecursiveImports(Map<String, List<String>> imports, String rootFilename, String filename) {
		if (imports.containsKey(filename)) {
			List<String> currentImports = imports.get(filename);
			if (currentImports.contains(rootFilename)) {
				return true;
			}
			for (String currImport : currentImports) {
				if (hasRecursiveImports(imports, rootFilename, currImport)) {
					return true;
				}
			}
		}
		return false;
	}

	private void addConfigurationSpecifiedImports(Map<String, ProtoFile> packageToProtoFileMap) {
		for (ProtoFile file : packageToProtoFileMap.values()) {
			for (String customImport : configuration.customImports) {
				boolean customImportInUse = false;

				List<String> qualifiedTypes = getFullyQualifiedTypes(customImport);
				typeloop: for (Type type : file.types()) {
					for (String qualifiedType : qualifiedTypes) {
						customImportInUse = isCustomImportInUseInNestedTypes(qualifiedType, type);
						if (customImportInUse) {
							break typeloop;
						}
					}
				}
				if (customImportInUse) {
					file.imports().add(customImport);
				}
			}

			if (configuration.includeValidationRules) {
				file.imports().add(VALIDATION_PROTO_IMPORT);
			}
			if (configuration.includeXsdOptions) {
				file.imports().add(XSDOPTIONS_PROTO_IMPORT);
			}

		}
	}

	private boolean isCustomImportInUseInNestedTypes(String importPackage, Type type) {
		AtomicBoolean customImportInUse = new AtomicBoolean(false);

		if (type instanceof MessageType) {
			for (Field field : ((MessageType) type).fieldsAndOneOfFields()) {
				if (field.getElementType() != null && field.getElementType().equalsIgnoreCase(importPackage)) {
					customImportInUse.set(true);
				}
			}
			if (!customImportInUse.get()) {
				messageTypes(type.nestedTypes()).forEach(mt -> {
					for (Field field : ((MessageType) type).fieldsAndOneOfFields()) {
						if (field.getElementType() != null && field.getElementType().equalsIgnoreCase(importPackage)) {
							customImportInUse.set(true);
						}
					}
					if (!customImportInUse.get()) {
						customImportInUse.set(isCustomImportInUseInNestedTypes(importPackage, mt));
					}

				});

			}
		}

		return customImportInUse.get();
	}

	private void computeLocalImports(Map<String, ProtoFile> packageToProtoFileMap) {
		for (ProtoFile file : packageToProtoFileMap.values()) {
			SortedSet<String> imports = new TreeSet<>(file.imports());

			messageTypes(file.types()).forEach(mt -> computeLocalImports(packageToProtoFileMap, file, imports, mt));

			file.imports().clear();
			file.imports().addAll(imports);

		}
	}

	private void computeLocalImports(Map<String, ProtoFile> packageToProtoFileMap, ProtoFile file, SortedSet<String> imports, MessageType messageType) {
		messageTypes(messageType.nestedTypes()).forEach(e -> computeLocalImports(packageToProtoFileMap, file, imports, e));

		for (Field field : messageType.fieldsAndOneOfFields()) {
			String packageName = StringUtils.trimToNull(field.packageName());
			if (file.packageName() != null && file.packageName().equals(packageName)) {
				field.clearPackageName();
			} else if (packageName != null) {
				// Add import
				ProtoFile fileToImport = packageToProtoFileMap.get(packageName);
				if (fileToImport != null) {
					imports.add(getPathFromPackageNameAndType(packageName, messageType) + File.separator + fileToImport.location().getPath());
				} else {
					LOGGER.error("Tried to create import for field packageName {}, but no such protofile exist", packageName);
				}
			}
		}
	}

	private void moveFieldPackageNameToFieldTypeName(Map<String, ProtoFile> packageToProtoFileMap) {
		for (ProtoFile file : packageToProtoFileMap.values()) {
			messageTypes(file.types()).forEach(this::moveFieldPackageNameToFieldTypeName);

		}
	}

	// Recursively loops through all fields for all nested types
	private void moveFieldPackageNameToFieldTypeName(MessageType messageType) {
		messageTypes(messageType.nestedTypes()).forEach(this::moveFieldPackageNameToFieldTypeName);

		List<Field> fields = messageType.fieldsAndOneOfFields();

		for (Field field : fields) {
			String fieldPackageName = StringUtils.trimToNull(field.packageName());
			if (fieldPackageName != null) {
				field.clearPackageName();
				field.updateElementType(fieldPackageName + "." + field.getElementType());
			}
		}
	}

	/*
	 * Adds leading '.' to field.elementType when needed. Ref.: https://developers.google.com/protocol-buffers/docs/proto3#packages-and-name-resolution
	 */
	private void addLeadingPeriodToElementType(Map<String, ProtoFile> packageToProtoFileMap) {
		for (ProtoFile file : packageToProtoFileMap.values()) {
			messageTypes(file.types()).forEach(mt -> {
				// TODO must this be done for nested types as well or handled differently?
				for (Field field : mt.fieldsAndOneOfFields()) {
					String fieldElementType = StringUtils.trimToNull(field.getElementType());
					if (fieldElementType != null && fieldElementType.contains(".")) {
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
			});
		}
	}

	private void translateTypes(Map<String, ProtoFile> packageToProtoFileMap) {
		for (ProtoFile file : packageToProtoFileMap.values()) {
			translateTypes(packageToProtoFileMap, file.types(), file.packageName());
		}
	}

	private void translateTypes(Map<String, ProtoFile> packageToProtoFileMap, List<Type> types, String packageName) {
		if (!types.isEmpty()) {
			Set<String> usedNames = findExistingTypeNamesInProtoFile(types);
			for (Type type : types) {
				if (type instanceof MessageType) {
					MessageType mt = (MessageType) type;

					translateTypes(packageToProtoFileMap, type.nestedTypes(), packageName);

					String messageName = mt.getName();
					String newMessageName = typeAndFieldNameMapper.translateType(messageName);

					if (!messageName.equals(newMessageName)) {
						if (!usedNames.contains(newMessageName)) {
							mt.updateName(newMessageName);
							usedNames.add(newMessageName);
							updateTypeReferences(packageToProtoFileMap, packageName, messageName, newMessageName);
						} else {
							LOGGER.warn("Cannot rename message {} to {} as type already exist! Renaming ignored", messageName, newMessageName);
						}

					}
					translateTypes(mt.fieldsAndOneOfFields());

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
							LOGGER.warn("Cannot rename enum {} to {} as type already exist! Renaming ignored", messageName, newMessageName);
						}
					}
				}
			}
		}
	}

	private void translateTypes(List<Field> fields) {
		for (Field field : fields) {
			// Translate basic types as well
			if (field.packageName() == null && basicTypes.contains(field.getElementType())) {
				String newFieldType = typeAndFieldNameMapper.translateType(field.getElementType());
				if (!newFieldType.equals(field.getElementType())) {
					LOGGER.debug("Replacing basicType {} with {}", field.getElementType(), newFieldType);
					field.updateElementType(newFieldType);
				}
			}

		}
	}

	private void replaceTypes(Map<String, ProtoFile> packageToProtoFileMap) {
		for (ProtoFile file : packageToProtoFileMap.values()) {
			replaceTypesRecursive(file.types());
		}
	}

	private void replaceTypesRecursive(List<Type> types) {
		messageTypes(types).forEach(mt -> {
			replaceTypesRecursive(mt.nestedTypes());
			replaceTypes(mt.fieldsAndOneOfFields());
		});

	}

	private void replaceTypes(List<Field> fields) {
		for (Field field : fields) {
			String newFieldType = typeAndFieldNameMapper.replaceType(field.getElementType());
			field.updateElementType(newFieldType);
		}
	}

	private void updateTypeReferences(Map<String, ProtoFile> packageToProtoFileMap, String packageNameOfType, String oldName, String newName) {
		for (ProtoFile file : packageToProtoFileMap.values()) {
			updateTypeReferences(packageNameOfType, oldName, newName, file.types(), file.packageName());
		}

	}

	private void updateTypeReferences(String packageNameOfType, String oldName, String newName, List<Type> types, String currentMessageTypePackage) {
		for (Type type : types) {
			updateTypeReferences(packageNameOfType, oldName, newName, type.nestedTypes(), currentMessageTypePackage);
			if (type instanceof MessageType) {
				MessageType mt = (MessageType) type;
				updateTypeReferences(packageNameOfType, oldName, newName, mt, mt.fieldsAndOneOfFields(), currentMessageTypePackage);
			}
		}
	}

	private void updateTypeReferences(String packageNameOfType, String oldName, String newName, MessageType mt, Collection<Field> fields,
			String currentMessageTypePackage) {
		for (Field field : fields) {
			if (samePackage(field.packageName(), packageNameOfType)) {
				String fieldType = field.getElementType();
				if (fieldType.equals(oldName)) {
					field.updateElementType(newName);
					LOGGER.debug("Updating field {} in type {} to {}", oldName, mt.getName(), newName);
				}
			}
		}

		Options options = mt.options();
		// Avoid concurrent mod exception

		List<OptionElement> listCopy = new ArrayList<>(options.getOptionElements());

		listCopy.stream().filter(e -> e.getName().equals(MessageType.XSD_BASE_TYPE_MESSAGE_OPTION_NAME)).forEach(e -> {
			String packageAndType = (String) e.getValue();
			if (packageAndType.equals(".")) {
				String packageName = packageAndType.substring(0, packageAndType.lastIndexOf('.'));
				String messageName = packageAndType.substring(packageName.hashCode() + 1);

				if (packageName.equals(packageAndType) && oldName.equals(messageName)) {
					options.replaceOption(MessageType.XSD_BASE_TYPE_MESSAGE_OPTION_NAME,
							new OptionElement(MessageType.XSD_BASE_TYPE_MESSAGE_OPTION_NAME, Kind.STRING, packageName + "." + newName, true));

				}
			} else if (currentMessageTypePackage.equals(packageNameOfType) && packageAndType.equals(oldName)) {
				options.replaceOption(MessageType.XSD_BASE_TYPE_MESSAGE_OPTION_NAME,
						new OptionElement(MessageType.XSD_BASE_TYPE_MESSAGE_OPTION_NAME, Kind.STRING, newName, true));
			}

		});
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
		for (ProtoFile file : packageToProtoFileMap.values()) {
			messageTypes(file.types()).forEach(mt -> {
				messageTypes(mt.nestedTypes()).forEach(e -> translateFieldNames(e.fieldsAndOneOfFields()));

				translateFieldNames(mt.fieldsAndOneOfFields());

			});

		}
	}

	private void translateFieldNames(List<Field> fields) {
		for (Field field : fields) {
			String fieldName = field.name();
			String newFieldName = typeAndFieldNameMapper.translateFieldName(fieldName);
			field.updateName(newFieldName);
		}
	}

	private void handleFieldNameCaseInsensitives(Map<String, ProtoFile> packageToProtoFileMap) {
		for (ProtoFile file : packageToProtoFileMap.values()) {

			messageTypes(file.types()).forEach(mt -> {

				messageTypes(mt.nestedTypes()).forEach(e -> handleFieldNameCaseInsensitives(e.fieldsAndOneOfFields()));

				handleFieldNameCaseInsensitives(mt.fieldsAndOneOfFields());

			});

		}
	}

	private void handleFieldNameCaseInsensitives(List<Field> fields) {
		Set<String> fieldNamesUppercase = new HashSet<>();

		for (Field field : fields) {
			String fieldName = field.name();
			boolean existedBefore = fieldNamesUppercase.add(fieldName.toUpperCase());
			if (!existedBefore) {
				fieldName = fieldName + UNDERSCORE + "v"; // TODO handles only one duplicate, many can exist
				field.updateName(fieldName);
			}
		}
	}

	private void underscoreFieldNames(Map<String, ProtoFile> packageToProtoFileMap) {
		for (ProtoFile file : packageToProtoFileMap.values()) {
			messageTypes(file.types()).forEach(this::underscoreFieldNames);
		}
	}

	private void underscoreFieldNames(MessageType mt) {

		messageTypes(mt.nestedTypes()).forEach(this::underscoreFieldNames);

		for (Field field : mt.fieldsAndOneOfFields()) {
			String fieldName = field.name();
			boolean startsWithUnderscore = fieldName.startsWith(UNDERSCORE);
			boolean endsWithUnderscore = fieldName.endsWith(UNDERSCORE);

			String strippedFieldName = StringUtils.removeEnd(StringUtils.removeStart(fieldName, UNDERSCORE), UNDERSCORE);

			String newFieldName = CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, strippedFieldName);

			// Remove all dashes
			newFieldName = StringUtils.remove(newFieldName, DASH);

			if (endsWithUnderscore) {
				newFieldName += "u"; // Trailing underscore not accepted by protoc for java
			}

			if (startsWithUnderscore) {
				newFieldName = UNDERSCORE + newFieldName;
			}

			field.updateName(newFieldName);
		}

	}

	private void escapeReservedJavaKeywords(Map<String, ProtoFile> packageToProtoFileMap) {
		for (ProtoFile file : packageToProtoFileMap.values()) {
			messageTypes(file.types()).forEach(mt -> {
				messageTypes(mt.nestedTypes()).forEach(this::escapeReservedJavaKeywords);
				escapeReservedJavaKeywords(mt);
			});

		}
	}

	private void escapeReservedJavaKeywords(MessageType mt) {
		for (Field field : mt.fieldsAndOneOfFields()) {
			String fieldName = field.name();
			String newFieldName = typeAndFieldNameMapper.escapeFieldName(fieldName);
			field.updateName(newFieldName);
		}
	}

	private static class ComponentMessageWrapper {
		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			ComponentMessageWrapper that = (ComponentMessageWrapper) o;
			return Objects.equals(xsComponent, that.xsComponent) && Objects.equals(messageName, that.messageName)
					&& Objects.equals(enclosingComplexType, that.enclosingComplexType);
		}

		@Override
		public int hashCode() {
			return Objects.hash(xsComponent, messageName, enclosingComplexType);
		}

		XSComponent xsComponent;
		XSComplexType enclosingComplexType;
		String messageName;

		public ComponentMessageWrapper(XSComponent xsComponent, XSComplexType enclosingComplexType, String messageName) {
			this.xsComponent = xsComponent;
			this.enclosingComplexType = enclosingComplexType;
			this.messageName = messageName;

		}

		@Override
		public String toString() {
			return "ComponentMessageWrapper{" + "xsComponent=" + xsComponent + ", enclosingComplexType=" + enclosingComplexType + ", messageName='"
					+ messageName + '\'' + '}';
		}
	}

	private void moveReusedLocalTypesToGlobal(Map<String, ProtoFile> packageToProtoFileMap, List<LocalType> localTypesAllPackages) {
		LOGGER.debug("Reorganizing embedded local types into global if reused");

		localTypesAllPackages.forEach(e -> LOGGER.debug("{} -> {}", e.enclosingType.getName(), e.localType.getName()));

		for (Entry<String, ProtoFile> protoFileEntry : packageToProtoFileMap.entrySet()) {

			String currentPackageName = protoFileEntry.getKey();

			Map<ComponentMessageWrapper, Integer> seenComplexTypesWithSameName = new TreeMap<>(Comparator.comparing(o -> o.messageName));

			// Filter local types that applies to this namespace/package
			List<LocalType> localTypes = localTypesAllPackages.stream().filter(e -> e.targetPackage.equals(currentPackageName)).collect(Collectors.toList());

			localTypes.forEach(e -> {
				ComponentMessageWrapper wrapper = new ComponentMessageWrapper(e.xsComponent, e.enclosingComplexType, e.localType.getName());

				Integer count = seenComplexTypesWithSameName.get(wrapper);
				if (count == null) {
					seenComplexTypesWithSameName.put(wrapper, 1);
				} else {
					count++;
					seenComplexTypesWithSameName.put(wrapper, count);
				}

			});

			seenComplexTypesWithSameName.forEach((key, value) -> LOGGER.debug("{} : {}", key, value));

			seenComplexTypesWithSameName.entrySet().stream().filter(e -> e.getValue() > 1).forEach(e -> {

				ComponentMessageWrapper currentComponent = e.getKey();

				LOGGER.debug("ComplexType/name reused: {} / {} times", currentComponent, e.getValue());

				// XSComplextype used more than once
				List<LocalType> usagesThisComponent = localTypes.stream()
						.filter(x -> currentComponent.messageName.equals(x.localType.getName()))
						.filter(x -> currentComponent.xsComponent == x.xsComponent)
						.filter(x -> currentComponent.enclosingComplexType == x.enclosingComplexType)
						.collect(Collectors.toList());

				LocalType first = usagesThisComponent.get(0);

				int numUniqueEnclosingTypes = usagesThisComponent.stream().map(u -> u.enclosingType.getName()).collect(Collectors.toSet()).size();
				if (numUniqueEnclosingTypes > 1) {

					List<LocalType> usagesOtherComponentsSameTypeName = localTypes.stream()
							.filter(x -> x.localType.getName().equals(currentComponent.messageName))
							.filter(x -> x.xsComponent != currentComponent.xsComponent)
							.collect(Collectors.toList());

					String packageName = first.targetPackage;
					if (packageName == null) {
						packageName = Schema2ProtoConfiguration.DEFAULT_PROTO_PACKAGE;
					}
					ProtoFile enclosingFile = packageToProtoFileMap.get(packageName);

					// DuplicateCheck
					String candidateName = currentComponent.messageName;

					if (!usagesOtherComponentsSameTypeName.isEmpty()) {
						Set<String> enclosingTypes = usagesThisComponent.stream().map(k -> k.enclosingComplexType.getName()).collect(Collectors.toSet());
						if (enclosingTypes.size() > 1) {
							throw new IllegalArgumentException(String.format(
									"Candidate enclosing types for %s are many - should be one %s. Cannot continue as conversion is not deterministic",
									first.localType.getName(), ToStringBuilder.reflectionToString(enclosingTypes.toArray(), ToStringStyle.SIMPLE_STYLE)));
						}
						candidateName = StringUtils.capitalize(enclosingTypes.iterator().next()) + "_"
								+ StringUtils.capitalize(first.localType.type().simpleName());
						LOGGER.debug("Candidate name for inherited local type prefixed with enclosing type {}", candidateName);
					} else {
						LOGGER.debug("Candidate name for inherited local type {} is unique - keeping as is", candidateName);
					}

					Optional<Type> existingType = checkForExistingType(enclosingFile, candidateName);
					if (!existingType.isPresent()) {
						MessageType localToBecomeGlobal = first.localType;
						localToBecomeGlobal.updateName(candidateName);
						enclosingFile.types().add(localToBecomeGlobal);

						// Remove all local types, update fields
						usagesThisComponent.forEach(y -> {
							y.enclosingType.nestedTypes().remove(y.localType);
							String previousElementType = y.referencingField.getElementType();
							y.referencingField.updateElementType(localToBecomeGlobal.getName());
							LOGGER.debug("In type {} field {} of type {} have now been replaced with package global type {}", y.enclosingType.getName(),
									y.referencingField.name(), previousElementType, localToBecomeGlobal.getName());
						});
					} else {
						LOGGER.warn(
								"Could not extract local type {} from {} using new global name {} due to an existing type with this name. This is a limitation of the current code but can be fixed with a better naming scheme",
								currentComponent.messageName, first.enclosingType.getName(), candidateName);
					}
				} else {
					LOGGER.debug("Not extracting {} due to single enclosing type {}", first.localType.getName(), first.enclosingType.getName());
				}
			});

		}
	}

	@NotNull
	private Optional<Type> checkForExistingType(ProtoFile enclosingFile, String candidateName) {
		Optional<Type> existingType = Optional.empty();
		for (Type type : enclosingFile.types()) {
			if (candidateName.equals(type.type().simpleName())) {
				existingType = Optional.of(type);
				break;
			}
		}
		return existingType;
	}

	private Stream<MessageType> messageTypes(List<Type> types) {
		return types.stream().filter(MessageType.class::isInstance).map(MessageType.class::cast);
	}

}
