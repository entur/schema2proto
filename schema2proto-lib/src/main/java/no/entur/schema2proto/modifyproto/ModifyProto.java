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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.google.common.collect.ImmutableList;
import com.squareup.wire.schema.EnumConstant;
import com.squareup.wire.schema.EnumType;
import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.IdentifierSet;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.Options;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.SchemaLoader;
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

public class ModifyProto {

	private static final Logger LOGGER = LoggerFactory.getLogger(ModifyProto.class);

	public static ModifyProtoConfiguration parseConfigurationFile(File configFile, File basedir) throws IOException, InvalidConfigurationException {
		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();

		try (InputStream in = Files.newInputStream(configFile.toPath())) {

			// Parse config file
			Constructor constructor = new Constructor(ModifyProtoConfigFile.class);
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
		SchemaLoader schemaLoader = new SchemaLoader();

		// Collect source proto files (but not dependencies). Used to know which files should be written to .proto and which that should remain a dependency.
		Collection<File> protoFiles = FileUtils.listFiles(configuration.inputDirectory, new String[] { "proto" }, true);
		List<String> protosLoaded = protoFiles.stream()
				.map(e -> configuration.inputDirectory.toURI().relativize(e.toURI()).getPath())
				.collect(Collectors.toList());

		for (String importRootFolder : configuration.customImportLocations) {
			schemaLoader.addSource(new File(configuration.basedir, importRootFolder).toPath());
		}

		schemaLoader.addSource(configuration.inputDirectory);

		for (Path s : schemaLoader.sources()) {
			LOGGER.info("Linking proto from path {}", s);
		}
		for (String s : schemaLoader.protos()) {
			LOGGER.info("Linking proto {}", s);
		}

		Schema schema = schemaLoader.load();

		// First run initial pruning, then look at the results and add referenced types from xsd.base_type

		IdentifierSet.Builder initialIdentifierSet = new IdentifierSet.Builder();
		initialIdentifierSet.exclude(configuration.excludes);
		initialIdentifierSet.include(configuration.includes);

		IdentifierSet finalIterationIdentifiers;

		if (configuration.includeBaseTypes) {
			finalIterationIdentifiers = followOneMoreLevel(initialIdentifierSet, schema);
		} else {
			finalIterationIdentifiers = initialIdentifierSet.build();
		}
		Schema prunedSchema = schema.prune(finalIterationIdentifiers);
		for (String s : finalIterationIdentifiers.unusedExcludes()) {
			LOGGER.warn("Unused exclude: {} (already excluded elsewhere or explicitly included?)", s);
		}
		for (String s : finalIterationIdentifiers.unusedIncludes()) {
			LOGGER.warn("Unused include: {} (already included elsewhere or explicitly excluded?) ", s);
		}

		for (NewField newField : configuration.newFields) {
			addField(newField, prunedSchema);
		}

		for (ModifyField modifyField : configuration.modifyFields) {
			modifyField(modifyField, prunedSchema);
		}

		for (NewEnumConstant newEnumValue : configuration.newEnumConstants) {
			addEnumConstant(newEnumValue, prunedSchema);
		}

		for (MergeFrom mergeFrom : configuration.mergeFrom) {
			mergeFromFile(mergeFrom, prunedSchema, configuration);
		}

		for (FieldOption fieldOption : configuration.fieldOptions) {
			addFieldOption(fieldOption, prunedSchema);
		}

		Set<Boolean> possibleIncompatibilitiesDetected = new HashSet<>();

		if (configuration.protoLockFile != null) {
			try {
				ProtolockBackwardsCompatibilityChecker backwardsCompatibilityChecker = new ProtolockBackwardsCompatibilityChecker();
				backwardsCompatibilityChecker.init(configuration.protoLockFile);
				ImmutableList<ProtoFile> files = prunedSchema.protoFiles();

				files.stream().forEach(file -> possibleIncompatibilitiesDetected.add(backwardsCompatibilityChecker.resolveBackwardIncompatibilities(file)));
			} catch (FileNotFoundException e) {
				throw new InvalidConfigurationException("Could not find proto.lock file, check configuration");
			}
		}

		if (configuration.includeGoPackageOptions) {
			includeGoPackageNameOptions(prunedSchema.protoFiles(), configuration.goPackageSourcePrefix);
		}

		Set<String> emptyImportLocations = protosLoaded.stream()
				.map(prunedSchema::protoFile)
				.filter(Objects::nonNull)
				.filter(this::isEmptyFile)
				.map(p -> p.location().getPath())
				.collect(Collectors.toSet());

		protosLoaded.stream().map(prunedSchema::protoFile).filter(Objects::nonNull).filter(p -> !isEmptyFile(p)).forEach(file -> {
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

	private void includeGoPackageNameOptions(Collection<ProtoFile> protoFiles, String goPackageSourcePrefix) {
		for (ProtoFile protoFile : protoFiles) {
			String optionName = "go_package";
			boolean alreadySet = protoFile.options().getOptionElements().stream().anyMatch(existingOption -> optionName.equals(existingOption.getName()));
			if (!alreadySet) {
				String goPackageName = packageNameToGoPackageName(goPackageSourcePrefix, protoFile.packageName());
				OptionElement optionElement = new OptionElement(optionName, OptionElement.Kind.STRING, goPackageName, false);
				protoFile.options().add(optionElement);
			}
		}
	}

	private boolean isEmptyFile(ProtoFile p) {
		return p.types().isEmpty() && p.getExtendList().isEmpty();
	}

	private IdentifierSet followOneMoreLevel(IdentifierSet.Builder identifierSetBuilder, Schema schema) {

		// Prune schema using current schema
		IdentifierSet identifierSet = identifierSetBuilder.build();
		Schema prunedSchema = schema.prune(identifierSet);

		// Add new base types to follow
		IdentifierSet.Builder updatedIdentifierSetBuilder = new IdentifierSet.Builder();
		updatedIdentifierSetBuilder.exclude(identifierSet.excludes());
		updatedIdentifierSetBuilder.include(identifierSet.includes());

		for (ProtoFile file : prunedSchema.protoFiles()) {
			for (Type t : file.types()) {
				includeBaseType(updatedIdentifierSetBuilder, t, file.packageName());
			}
		}

		IdentifierSet updatedIdentifierSet = updatedIdentifierSetBuilder.build();

		// More dependencies found, iterate once more
		if (!identifierSet.includes().equals(updatedIdentifierSet.includes())) {
			return followOneMoreLevel(updatedIdentifierSetBuilder, schema);
		} else {
			// Another iteration yielded no more identifiers to include, we're done
			return updatedIdentifierSet;
		}

	}

	private void includeBaseType(IdentifierSet.Builder b, Type type, String enclosingPackage) {
		if (type.options() != null) {
			List<OptionElement> baseTypeInherits = type.options()
					.getOptionElements()
					.stream()
					.filter(e -> e.getName().equals(MessageType.XSD_BASE_TYPE_MESSAGE_OPTION_NAME))
					.collect(Collectors.toList());
			baseTypeInherits.stream().forEach(e -> {
				String baseTypeValue = (String) e.getValue();
				if (baseTypeValue.contains(".")) {
					b.include(baseTypeValue);
				} else {
					// No package in includeBaseType statement
					String fullType = baseTypeValue;
					if (enclosingPackage != null) {
						fullType = enclosingPackage + "." + fullType;
					}
					b.include(fullType);
				}
			});
		}
		type.nestedTypes().stream().forEach(e -> includeBaseType(b, e, enclosingPackage));

	}

	private void mergeFromFile(MergeFrom mergeFrom, Schema prunedSchema, ModifyProtoConfiguration configuration) throws IOException {

		SchemaLoader schemaLoader = new SchemaLoader();

		for (String importRootFolder : configuration.customImportLocations) {
			schemaLoader.addSource(new File(configuration.basedir, importRootFolder).toPath());
		}

		if (mergeFrom.sourceFolder.isAbsolute()) {
			schemaLoader.addSource(mergeFrom.sourceFolder);
		} else {
			schemaLoader.addSource(new File(configuration.basedir, mergeFrom.sourceFolder.getPath()));
		}

		if (configuration.inputDirectory.isAbsolute()) {
			schemaLoader.addSource(configuration.inputDirectory);
		} else {
			schemaLoader.addSource(new File(configuration.basedir, configuration.inputDirectory.getPath()));
		}

		schemaLoader.addProto(mergeFrom.protoFile);

		Schema schema = schemaLoader.load();

		ProtoFile source = schema.protoFile(mergeFrom.protoFile);
		ProtoFile destination = prunedSchema.protoFileForPackage(source.packageName());

		if (destination == null) {
			throw new IllegalArgumentException("Destination protofile not found");
		} else {
			destination.mergeFrom(source);
		}

	}

	private void addEnumConstant(NewEnumConstant newEnumConstant, Schema prunedSchema) throws InvalidProtobufException {
		Type targetEnumType = prunedSchema.getType(newEnumConstant.targetEnumType);
		if (targetEnumType instanceof EnumType) {
			List<OptionElement> optionElements = new ArrayList<>();
			Options options = new Options(Options.ENUM_VALUE_OPTIONS, optionElements);
			Location location = new Location("", "", -1, -1);
			EnumConstant enumConstant = new EnumConstant(location, newEnumConstant.name, newEnumConstant.fieldNumber, newEnumConstant.documentation, options);
			EnumType enumType = (EnumType) targetEnumType;
			// Duplicate check
			Optional<EnumConstant> existing = enumType.constants()
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

	private void addField(NewField newField, Schema prunedSchema) throws InvalidProtobufException {

		MessageType type = (MessageType) prunedSchema.getType(newField.targetMessageType);
		if (type == null) {
			throw new InvalidProtobufException("Did not find existing type " + newField.targetMessageType);
		} else {

			List<OptionElement> optionElements = new ArrayList<>();
			Options options = new Options(Options.FIELD_OPTIONS, optionElements);
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

			Field field = new Field(fieldPackage, location, label, newField.name, StringUtils.trimToEmpty(newField.documentation), tag, null, newField.type,
					options, false, false);
			List<Field> updatedFields = new ArrayList<>(type.fields());
			updatedFields.add(field);
			type.setDeclaredFields(updatedFields);

			String importStatement = StringUtils.trimToNull(newField.importProto);

			if (importStatement != null) {
				String targetPackageName = StringUtils.trimToNull(StringUtils.substringBeforeLast(newField.targetMessageType, "."));
				ProtoFile targetFile = prunedSchema.protoFileForPackage(targetPackageName);
				if (newField.targetMessageType.equals(targetPackageName)) {
					// no package name on target
					targetFile = prunedSchema.protoFileForPackage(null);
				}
				targetFile.imports().add(importStatement);
			}

		}

	}

	private void modifyField(ModifyField modifyField, Schema prunedSchema) throws InvalidProtobufException {
		MessageType type = (MessageType) prunedSchema.getType(modifyField.targetMessageType);
		if (type == null) {
			throw new InvalidProtobufException("Did not find existing type " + modifyField.targetMessageType);
		}
		Field field = type.field(modifyField.field);
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

	public void addFieldOption(FieldOption fieldOption, Schema prunedSchema) throws InvalidProtobufException {
		MessageType type = (MessageType) prunedSchema.getType(fieldOption.targetMessageType);
		if (type == null) {
			throw new InvalidProtobufException("Did not find existing type " + fieldOption.targetMessageType);
		}
		Field field = type.field(fieldOption.field);
		if (field == null) {
			throw new InvalidProtobufException("Did not find existing field " + fieldOption.field);
		}
		if (StringUtils.isEmpty(fieldOption.option)) {
			throw new InvalidProtobufException("Missing option for field " + fieldOption.field);
		}
		OptionReader reader = new OptionReader(new SyntaxReader(fieldOption.option.toCharArray(), null));
		reader.readOptions().forEach(option -> field.options().add(option));

	}

}
