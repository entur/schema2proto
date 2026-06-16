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
package no.entur.schema2proto.modifyproto;

import static no.entur.schema2proto.generateproto.GoPackageNameHelper.packageNameToGoPackageName;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.PruningRules;
import com.squareup.wire.schema.Reserved;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.Type;
import com.squareup.wire.schema.internal.parser.OptionElement;
import com.squareup.wire.schema.internal.parser.OptionReader;
import com.squareup.wire.schema.internal.parser.SyntaxReader;

import no.entur.schema2proto.InvalidConfigurationException;
import no.entur.schema2proto.compatibility.BackwardsCompatibilityCheckException;
import no.entur.schema2proto.compatibility.ProtolockBackwardsCompatibilityChecker;
import no.entur.schema2proto.modifyproto.config.FieldOption;
import no.entur.schema2proto.modifyproto.config.MergeFrom;
import no.entur.schema2proto.modifyproto.config.ModifyField;
import no.entur.schema2proto.modifyproto.config.ModifyProtoConfiguration;
import no.entur.schema2proto.modifyproto.config.NewEnumConstant;
import no.entur.schema2proto.modifyproto.config.NewField;
import no.entur.schema2proto.wire.MutableEnumConstant;
import no.entur.schema2proto.wire.MutableEnumType;
import no.entur.schema2proto.wire.MutableField;
import no.entur.schema2proto.wire.MutableMessageType;
import no.entur.schema2proto.wire.MutableOptions;
import no.entur.schema2proto.wire.MutableProtoFile;
import no.entur.schema2proto.wire.MutableType;
import no.entur.schema2proto.wire.WireBuilders;
import no.entur.schema2proto.wire.WireSchemaLoader;

public class ModifyProto {

	private static final Logger LOGGER = LoggerFactory.getLogger(ModifyProto.class);

	public static ModifyProtoConfiguration parseConfigurationFile(File configFile, File basedir) throws IOException, InvalidConfigurationException {
		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();

		try (InputStream in = Files.newInputStream(configFile.toPath())) {

			// Parse config file
			Constructor constructor = new Constructor(ModifyProtoConfigFile.class, new LoaderOptions());
			TypeDescription customTypeDescription = new TypeDescription(ModifyProtoConfigFile.class);
			customTypeDescription.addPropertyParameters("newFields", NewField.class);
			customTypeDescription.addPropertyParameters("modifyFields", ModifyField.class);
			customTypeDescription.addPropertyParameters("mergeFrom", MergeFrom.class);
			customTypeDescription.addPropertyParameters("valdiationRules", FieldOption.class);
			constructor.addTypeDescription(customTypeDescription);
			Yaml yaml = new Yaml(constructor);

			LOGGER.info("Using configFile {}", configFile);
			ModifyProtoConfigFile config = yaml.load(in);

			// Check config values
			if (config.outputDirectory == null) {
				throw new InvalidConfigurationException("No output directory");
			} else {
				configuration.outputDirectory = new File(basedir, config.outputDirectory);
				configuration.outputDirectory.mkdirs();
			}

			if (config.inputDirectory == null) {
				throw new InvalidConfigurationException("no input directory");
			} else {
				configuration.inputDirectory = new File(basedir, config.inputDirectory);
			}

			if (config.includes != null) {
				configuration.includes.addAll(config.includes.stream().filter(e -> StringUtils.trimToNull(e) != null).collect(Collectors.toList()));
			}

			if (config.excludes != null) {
				configuration.excludes.addAll(config.excludes.stream().filter(e -> StringUtils.trimToNull(e) != null).collect(Collectors.toList()));
			}

			if (config.mergeFrom != null) {
				configuration.mergeFrom = new ArrayList<>(config.mergeFrom);
			}

			if (config.newFields != null) {
				configuration.newFields = new ArrayList<>(config.newFields);
			}

			if (config.modifyFields != null) {
				configuration.modifyFields = new ArrayList<>(config.modifyFields);
			}

			if (config.newEnumConstants != null) {
				configuration.newEnumConstants = new ArrayList<>(config.newEnumConstants);
			}

			if (config.fieldOptions != null) {
				configuration.fieldOptions = new ArrayList<>(config.fieldOptions);
			}

			configuration.includeBaseTypes = config.includeBaseTypes;
			if (config.protoLockFile != null) {
				configuration.protoLockFile = new File(basedir, config.protoLockFile);
			}

			configuration.failIfRemovedFields = config.failIfRemovedFields;

			if (config.customImportLocations != null) {
				configuration.customImportLocations = new ArrayList<>(
						config.customImportLocations.stream().filter(e -> StringUtils.trimToNull(e) != null).collect(Collectors.toList()));
			}

			configuration.basedir = basedir;

			configuration.includeGoPackageOptions = config.includeGoPackageOptions;
			configuration.goPackageSourcePrefix = config.goPackageSourcePrefix;

		}

		return configuration;
	}

	public void modifyProto(ModifyProtoConfiguration configuration) throws IOException, InvalidProtobufException, InvalidConfigurationException {

		// Collect source proto files (but not dependencies). Used to know which files should be written to .proto and which that should remain a dependency.
		Collection<File> protoFiles = FileUtils.listFiles(configuration.inputDirectory, new String[] { "proto" }, true);
		List<String> protosLoaded = protoFiles.stream()
				.map(e -> configuration.inputDirectory.toURI().relativize(e.toURI()).getPath())
				.collect(Collectors.toList());

		List<java.nio.file.Path> sources = new ArrayList<>();
		for (String importRootFolder : configuration.customImportLocations) {
			sources.add(new File(configuration.basedir, importRootFolder).toPath());
		}
		sources.add(configuration.inputDirectory.toPath());

		for (java.nio.file.Path s : sources) {
			LOGGER.info("Linking proto from path {}", s);
		}

		Schema schema = WireSchemaLoader.load(sources, Collections.emptyList());

		// First run initial pruning, then look at the results and add referenced types from xsd.base_type
		Set<String> excludes = new LinkedHashSet<>(configuration.excludes);
		Set<String> includes = new LinkedHashSet<>(configuration.includes);

		if (configuration.includeBaseTypes) {
			includes = followOneMoreLevel(includes, excludes, schema);
		}

		PruningRules finalIterationRules = buildPruningRules(includes, excludes);
		Schema prunedSchema = schema.prune(finalIterationRules);
		for (String s : finalIterationRules.unusedPrunes()) {
			LOGGER.warn("Unused exclude: {} (already excluded elsewhere or explicitly included?)", s);
		}
		for (String s : finalIterationRules.unusedRoots()) {
			LOGGER.warn("Unused include: {} (already included elsewhere or explicitly excluded?) ", s);
		}

		// Convert the pruned (immutable) schema into the mutable builder model used for editing and backwards-compat resolution
		Map<String, MutableProtoFile> builderFilesByPath = new LinkedHashMap<>();
		for (ProtoFile pf : prunedSchema.getProtoFiles()) {
			builderFilesByPath.put(pf.getLocation().getPath(), WireBuilders.fromProtoFile(pf));
		}
		Collection<MutableProtoFile> builderFiles = builderFilesByPath.values();

		for (NewField newField : configuration.newFields) {
			addField(newField, builderFiles);
		}

		for (ModifyField modifyField : configuration.modifyFields) {
			modifyField(modifyField, builderFiles);
		}

		for (NewEnumConstant newEnumValue : configuration.newEnumConstants) {
			addEnumConstant(newEnumValue, builderFiles);
		}

		for (MergeFrom mergeFrom : configuration.mergeFrom) {
			mergeFromFile(mergeFrom, builderFiles, configuration);
		}

		for (FieldOption fieldOption : configuration.fieldOptions) {
			addFieldOption(fieldOption, builderFiles);
		}

		Set<Boolean> possibleIncompatibilitiesDetected = new HashSet<>();

		if (configuration.protoLockFile != null) {
			try {
				ProtolockBackwardsCompatibilityChecker backwardsCompatibilityChecker = new ProtolockBackwardsCompatibilityChecker();
				backwardsCompatibilityChecker.init(configuration.protoLockFile);
				for (MutableProtoFile file : builderFiles) {
					possibleIncompatibilitiesDetected.add(backwardsCompatibilityChecker.resolveBackwardIncompatibilities(file));
				}
			} catch (FileNotFoundException e) {
				throw new InvalidConfigurationException("Could not find proto.lock file, check configuration");
			}
		}

		if (configuration.includeGoPackageOptions) {
			includeGoPackageNameOptions(builderFiles, configuration.goPackageSourcePrefix);
		}

		Set<String> emptyImportLocations = protosLoaded.stream()
				.map(builderFilesByPath::get)
				.filter(Objects::nonNull)
				.filter(this::isEmptyFile)
				.map(p -> p.location().getPath())
				.collect(Collectors.toSet());

		protosLoaded.stream().map(builderFilesByPath::get).filter(Objects::nonNull).filter(p -> !isEmptyFile(p)).forEach(file -> {
			file.imports().removeIf(emptyImportLocations::contains);
			file.publicImports().removeIf(emptyImportLocations::contains);
			File outputFile = new File(configuration.outputDirectory, file.location().getPath());
			outputFile.getParentFile().mkdirs();
			try (Writer writer = new FileWriter(outputFile)) {
				writer.write(file.toSchema());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			LOGGER.info("Wrote file {}", outputFile.getPath());

		});

		if (configuration.failIfRemovedFields && possibleIncompatibilitiesDetected.contains(Boolean.TRUE)) {
			throw new BackwardsCompatibilityCheckException(
					"Backwards incompatibilities detected. Check warnings messages above. To ignore warnings, rerun with -DfailIfRemovedFields=false");
		}

	}

	private void includeGoPackageNameOptions(Collection<MutableProtoFile> protoFiles, String goPackageSourcePrefix) {
		for (MutableProtoFile protoFile : protoFiles) {
			String optionName = "go_package";
			boolean alreadySet = protoFile.options().getOptionElements().stream().anyMatch(existingOption -> optionName.equals(existingOption.getName()));
			if (!alreadySet) {
				String goPackageName = packageNameToGoPackageName(goPackageSourcePrefix, protoFile.packageName());
				OptionElement optionElement = new OptionElement(optionName, OptionElement.Kind.STRING, goPackageName, false);
				protoFile.options().add(optionElement);
			}
		}
	}

	private boolean isEmptyFile(MutableProtoFile p) {
		return p.types().isEmpty() && p.getExtendList().isEmpty();
	}

	/**
	 * Builds the pruning rules, mirroring the vendored {@code IdentifierSet} semantics where excludes take precedence over includes: an identifier present in
	 * both is pruned, not rooted. Stock wire's {@link PruningRules} rejects the same identifier in both roots and prunes, so the overlap is dropped from the
	 * roots here.
	 */
	private PruningRules buildPruningRules(Set<String> includes, Set<String> excludes) {
		PruningRules.Builder builder = new PruningRules.Builder();
		builder.prune(excludes);
		builder.addRoot(includes.stream().filter(i -> !excludes.contains(i)).collect(Collectors.toList()));
		return builder.build();
	}

	private Set<String> followOneMoreLevel(Set<String> includes, Set<String> excludes, Schema schema) {

		// Prune schema using current rules
		Schema prunedSchema = schema.prune(buildPruningRules(includes, excludes));

		// Add new base types to follow
		Set<String> updatedIncludes = new LinkedHashSet<>(includes);
		for (ProtoFile file : prunedSchema.getProtoFiles()) {
			for (Type t : file.getTypes()) {
				includeBaseType(updatedIncludes, t, file.getPackageName());
			}
		}

		// More dependencies found, iterate once more
		if (!includes.equals(updatedIncludes)) {
			return followOneMoreLevel(updatedIncludes, excludes, schema);
		} else {
			// Another iteration yielded no more identifiers to include, we're done
			return updatedIncludes;
		}

	}

	private void includeBaseType(Set<String> includes, Type type, String enclosingPackage) {
		if (type.getOptions() != null) {
			List<OptionElement> baseTypeInherits = type.getOptions()
					.getElements()
					.stream()
					.filter(e -> e.getName().equals(MutableMessageType.XSD_BASE_TYPE_MESSAGE_OPTION_NAME))
					.collect(Collectors.toList());
			baseTypeInherits.stream().forEach(e -> {
				String baseTypeValue = (String) e.getValue();
				if (baseTypeValue.contains(".")) {
					includes.add(baseTypeValue);
				} else {
					// No package in includeBaseType statement
					String fullType = baseTypeValue;
					if (enclosingPackage != null) {
						fullType = enclosingPackage + "." + fullType;
					}
					includes.add(fullType);
				}
			});
		}
		type.getNestedTypes().stream().forEach(e -> includeBaseType(includes, e, enclosingPackage));

	}

	private void mergeFromFile(MergeFrom mergeFrom, Collection<MutableProtoFile> builderFiles, ModifyProtoConfiguration configuration) throws IOException {

		List<java.nio.file.Path> sources = new ArrayList<>();

		for (String importRootFolder : configuration.customImportLocations) {
			sources.add(new File(configuration.basedir, importRootFolder).toPath());
		}

		if (mergeFrom.sourceFolder.isAbsolute()) {
			sources.add(mergeFrom.sourceFolder.toPath());
		} else {
			sources.add(new File(configuration.basedir, mergeFrom.sourceFolder.getPath()).toPath());
		}

		if (configuration.inputDirectory.isAbsolute()) {
			sources.add(configuration.inputDirectory.toPath());
		} else {
			sources.add(new File(configuration.basedir, configuration.inputDirectory.getPath()).toPath());
		}

		Schema schema = WireSchemaLoader.load(sources, Collections.singletonList(mergeFrom.protoFile));

		ProtoFile source = schema.protoFile(mergeFrom.protoFile);
		MutableProtoFile sourceBuilder = WireBuilders.fromProtoFile(source);
		MutableProtoFile destination = findProtoFileForPackage(builderFiles, sourceBuilder.packageName());

		if (destination == null) {
			throw new IllegalArgumentException("Destination protofile not found");
		} else {
			destination.mergeFrom(sourceBuilder);
		}

	}

	private void addEnumConstant(NewEnumConstant newEnumConstant, Collection<MutableProtoFile> builderFiles) throws InvalidProtobufException {
		MutableEnumType enumType = findEnumType(builderFiles, newEnumConstant.targetEnumType);
		if (enumType != null) {
			MutableOptions options = new MutableOptions(MutableOptions.ENUM_VALUE_OPTIONS, new ArrayList<>());
			Location location = new Location("", "", -1, -1);
			MutableEnumConstant enumConstant = new MutableEnumConstant(location, newEnumConstant.name, newEnumConstant.fieldNumber,
					newEnumConstant.documentation, options);
			// Duplicate check
			Optional<MutableEnumConstant> existing = enumType.constants()
					.stream()
					.filter(e -> e.getName().equals(enumConstant.getName()) || e.getTag() == enumConstant.getTag())
					.findFirst();
			if (existing.isPresent()) {
				throw new InvalidProtobufException("Enum constant already present: " + newEnumConstant);
			} else {
				enumType.constants().add(enumConstant);
			}
		} else {
			throw new InvalidProtobufException("Did not find existing enum " + newEnumConstant.targetEnumType);
		}
	}

	private void addField(NewField newField, Collection<MutableProtoFile> builderFiles) throws InvalidProtobufException {

		MutableMessageType type = findMessageType(builderFiles, newField.targetMessageType);
		if (type == null) {
			throw new InvalidProtobufException("Did not find existing type " + newField.targetMessageType);
		} else {

			// Check if field name or tag is reserved, if so remove reservation if allowIfReserved is set, otherwise throw exception
			List<Reserved> reservedFields = type.getReserveds();
			boolean nameReserved = reservedFields.stream().anyMatch(r -> r.matchesName(newField.name));
			boolean tagReserved = newField.fieldNumber != -1 && reservedFields.stream().anyMatch(r -> r.matchesTag(newField.fieldNumber));

			if (nameReserved || tagReserved) {
				if (!newField.allowIfReserved) {
					throw new InvalidProtobufException("Field name '" + newField.name + "' and/or fieldNumber " + newField.fieldNumber + " is reserved in type "
							+ newField.targetMessageType + ". Use allowIfReserved to override.");
				}
				// Remove only the matching name/tag values from each Reserved entry, keeping other values intact
				List<Reserved> updatedReservedFields = new ArrayList<>();
				for (Reserved reserved : reservedFields) {
					List<Object> filteredValues = reserved.getValues()
							.stream()
							.filter(v -> !Objects.equals(v, newField.name) && !Objects.equals(v, newField.fieldNumber))
							.collect(Collectors.toList());

					if (!filteredValues.isEmpty()) {
						updatedReservedFields.add(new Reserved(reserved.getLocation(), reserved.getDocumentation(), filteredValues));
					}
				}
				reservedFields.clear();
				reservedFields.addAll(updatedReservedFields);
			}

			MutableOptions options = new MutableOptions(MutableOptions.FIELD_OPTIONS, new ArrayList<>());
			int tag = newField.fieldNumber;

			String fieldPackage = StringUtils.substringBeforeLast(newField.type, ".");

			if (fieldPackage.equals(newField.type)) {
				// no package
				fieldPackage = null;
			}

			Field.Label label = null;
			if (StringUtils.trimToNull(newField.label) != null) {
				label = Field.Label.valueOf(newField.label.toUpperCase());
			}
			Location location = new Location("", "", -1, -1);

			MutableField field = new MutableField(fieldPackage, location, label, newField.name, StringUtils.trimToEmpty(newField.documentation), tag, null,
					newField.type, options, false, false);
			List<MutableField> updatedFields = new ArrayList<>(type.fields());
			updatedFields.add(field);
			type.setDeclaredFields(updatedFields);

			String importStatement = StringUtils.trimToNull(newField.importProto);

			if (importStatement != null) {
				String targetPackageName = StringUtils.trimToNull(StringUtils.substringBeforeLast(newField.targetMessageType, "."));
				MutableProtoFile targetFile = findProtoFileForPackage(builderFiles, targetPackageName);
				if (newField.targetMessageType.equals(targetPackageName)) {
					// no package name on target
					targetFile = findProtoFileForPackage(builderFiles, null);
				}
				targetFile.imports().add(importStatement);
			}

		}

	}

	private void modifyField(ModifyField modifyField, Collection<MutableProtoFile> builderFiles) throws InvalidProtobufException {
		MutableMessageType type = findMessageType(builderFiles, modifyField.targetMessageType);
		if (type == null) {
			throw new InvalidProtobufException("Did not find existing type " + modifyField.targetMessageType);
		}
		MutableField field = type.field(modifyField.field);
		if (field == null) {
			throw new InvalidProtobufException("Did not find existing field " + modifyField.field);
		}
		if (modifyField.documentation != null) {
			if (modifyField.documentationPattern != null) {
				field.updateDocumentation(field.documentation().replaceAll(modifyField.documentationPattern, modifyField.documentation));
			} else {
				field.updateDocumentation(modifyField.documentation);
			}
		}
	}

	public void addFieldOption(FieldOption fieldOption, Collection<MutableProtoFile> builderFiles) throws InvalidProtobufException {
		MutableMessageType type = findMessageType(builderFiles, fieldOption.targetMessageType);
		if (type == null) {
			throw new InvalidProtobufException("Did not find existing type " + fieldOption.targetMessageType);
		}
		MutableField field = type.field(fieldOption.field);
		if (field == null) {
			throw new InvalidProtobufException("Did not find existing field " + fieldOption.field);
		}
		if (StringUtils.isEmpty(fieldOption.option)) {
			throw new InvalidProtobufException("Missing option for field " + fieldOption.field);
		}
		OptionReader reader = new OptionReader(new SyntaxReader(fieldOption.option.toCharArray(), Location.get("", "")));
		reader.readOptions().forEach(option -> field.options().add(option));

		// A field that uses the buf.validate extension must import its definition for the result to be valid protobuf
		if (fieldOption.option.contains("buf.validate")) {
			MutableProtoFile file = findProtoFileForType(builderFiles, fieldOption.targetMessageType);
			if (file != null && !file.imports().contains("buf/validate/validate.proto")) {
				file.imports().add("buf/validate/validate.proto");
			}
		}
	}

	private MutableProtoFile findProtoFileForType(Collection<MutableProtoFile> builderFiles, String qualifiedName) {
		for (MutableProtoFile file : builderFiles) {
			if (findType(file.types(), qualifiedName) != null) {
				return file;
			}
		}
		return null;
	}

	private MutableMessageType findMessageType(Collection<MutableProtoFile> builderFiles, String qualifiedName) {
		for (MutableProtoFile file : builderFiles) {
			MutableType type = findType(file.types(), qualifiedName);
			if (type instanceof MutableMessageType) {
				return (MutableMessageType) type;
			}
		}
		return null;
	}

	private MutableEnumType findEnumType(Collection<MutableProtoFile> builderFiles, String qualifiedName) {
		for (MutableProtoFile file : builderFiles) {
			MutableType type = findType(file.types(), qualifiedName);
			if (type instanceof MutableEnumType) {
				return (MutableEnumType) type;
			}
		}
		return null;
	}

	private MutableType findType(List<MutableType> types, String qualifiedName) {
		for (MutableType type : types) {
			if (type.type().toString().equals(qualifiedName)) {
				return type;
			}
			MutableType nested = findType(type.nestedTypes(), qualifiedName);
			if (nested != null) {
				return nested;
			}
		}
		return null;
	}

	private MutableProtoFile findProtoFileForPackage(Collection<MutableProtoFile> builderFiles, String packageName) {
		for (MutableProtoFile file : builderFiles) {
			if (Objects.equals(file.packageName(), packageName)) {
				return file;
			}
		}
		return null;
	}

}
