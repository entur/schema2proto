package no.entur.schema2proto.modifyproto;

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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.IdentifierSet;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.Options;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.SchemaLoader;
import com.squareup.wire.schema.internal.parser.OptionElement;

import no.entur.schema2proto.InvalidConfigurationException;
import no.entur.schema2proto.generateproto.Schema2Proto;

public class ModifyProto {

	private static final Logger LOGGER = LoggerFactory.getLogger(Schema2Proto.class);

	public void modifyProto(File configFile, File basedir) throws IOException, InvalidConfigurationException {

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();

		try (InputStream in = Files.newInputStream(configFile.toPath())) {

			Constructor constructor = new Constructor(ModifyProtoConfigFile.class);
			TypeDescription customTypeDescription = new TypeDescription(ModifyProtoConfigFile.class);
			customTypeDescription.addPropertyParameters("newFields", NewField.class);
			customTypeDescription.addPropertyParameters("mergeFrom", MergeFrom.class);
			constructor.addTypeDescription(customTypeDescription);
			Yaml yaml = new Yaml(constructor);

			LOGGER.info("Using configFile {}", configFile);

			ModifyProtoConfigFile config = yaml.load(in);

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
				configuration.includes.addAll(config.includes);
			}

			if (config.excludes != null) {
				configuration.excludes.addAll(config.excludes);
			}

			if (config.mergeFrom != null) {
				configuration.mergeFrom = new ArrayList<>(config.mergeFrom);
			}

			if (config.newFields != null) {
				configuration.newFields = new ArrayList<>(config.newFields);
			}

			if (config.customImportLocations != null) {
				configuration.customImportLocations = new ArrayList<>(config.customImportLocations);
			}

			configuration.basedir = basedir;

			modifyProto(configuration);

		}
	}

	public void modifyProto(ModifyProtoConfiguration configuration) throws IOException {
		SchemaLoader schemaLoader = new SchemaLoader();

		Collection<File> protoFiles = FileUtils.listFiles(configuration.inputDirectory, new String[] { "proto" }, true);
		List<String> protosLoaded = protoFiles.stream()
				.map(e -> configuration.inputDirectory.toURI().relativize(e.toURI()).getPath())
				.collect(Collectors.toList());

		for (String importRootFolder : configuration.customImportLocations) {
			schemaLoader.addSource(new File(configuration.basedir, importRootFolder).toPath());
		}

		schemaLoader.addSource(configuration.inputDirectory);

		for (Path s : schemaLoader.sources()) {
			LOGGER.info("Linking proto from path " + s);
		}
		for (String s : schemaLoader.protos()) {
			LOGGER.info("Linking proto " + s);
		}

		Schema schema = schemaLoader.load();

		IdentifierSet.Builder b = new IdentifierSet.Builder();
		b.exclude(configuration.excludes);
		b.include(configuration.includes);

		Schema prunedSchema = schema.prune(b.build());

		for (NewField newField : configuration.newFields) {
			addField(newField, prunedSchema);
		}

		for (MergeFrom mergeFrom : configuration.mergeFrom) {
			mergeFromFile(mergeFrom, prunedSchema, configuration);
		}

		for (String protoPathLoaded : protosLoaded) {
			ProtoFile protoFile = prunedSchema.protoFile(protoPathLoaded);
			File outputFile = new File(configuration.outputDirectory, protoFile.location().getPath());
			outputFile.getParentFile().mkdirs();
			Writer writer = new FileWriter(outputFile);
			writer.write(protoFile.toSchema());
			writer.close();

			LOGGER.info("Wrote file " + outputFile.getPath());

		}

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

	private void addField(NewField newField, Schema prunedSchema) {

		MessageType type = (MessageType) prunedSchema.getType(newField.targetMessageType);
		if (type == null) {
			LOGGER.error("Did not find existing type " + newField.targetMessageType);
		} else {

			List<OptionElement> optionElements = new ArrayList<OptionElement>();

			Options options = new Options(Options.FIELD_OPTIONS, optionElements);
			int tag = newField.fieldNumber;

			String fieldPackage = StringUtils.substringBeforeLast(newField.type, ".");
			String fieldType = StringUtils.substringAfterLast(newField.type, ".");

			if (fieldPackage.equals(newField.type)) {
				// no package
				fieldType = fieldPackage;
				fieldPackage = null;
			}

			Field.Label label = null;
			if (StringUtils.trimToNull(newField.label) != null) {
				label = Field.Label.valueOf(newField.label.toUpperCase());
			}
			Location location = new Location("", "", -1, -1);

			Field field = new Field(fieldPackage, location, label, newField.name, StringUtils.trimToEmpty(newField.documentation), tag, null, fieldType,
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

}
