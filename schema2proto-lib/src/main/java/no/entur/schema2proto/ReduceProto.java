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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import com.squareup.wire.schema.IdentifierSet;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.SchemaLoader;

public class ReduceProto {

	private static final Logger LOGGER = LoggerFactory.getLogger(Schema2Proto.class);

	public void reduceProto(File configFile, File basedir) throws IOException, InvalidConfigurationException {

		ReduceProtoConfiguration configuration = new ReduceProtoConfiguration();

		try (InputStream in = Files.newInputStream(configFile.toPath())) {
			LOGGER.info("Using configFile {}", configFile);
			Yaml yaml = new Yaml();
			ReduceProtoConfigFile config = yaml.loadAs(in, ReduceProtoConfigFile.class);

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

			if ((config.includes == null) && config.excludes == null) {
				throw new InvalidConfigurationException("No includes/excludes - why are you running this tool?");
			} else {

				if (config.includes != null) {
					configuration.includes.addAll(config.includes);
				}

				if (config.excludes != null) {
					configuration.excludes.addAll(config.excludes);
				}

			}

			if (config.customImportLocations != null) {
				configuration.customImportLocations = new ArrayList<>(config.customImportLocations);
			}

			configuration.includeValidationRules = config.includeValidationRules;

			configuration.basedir = basedir;

			reduceProto(configuration);

		}
	}

	public void reduceProto(ReduceProtoConfiguration configuration) throws IOException {
		SchemaLoader schemaLoader = new SchemaLoader();

		Collection<File> protoFiles = FileUtils.listFiles(configuration.inputDirectory, new String[] { "proto" }, true);
		List<String> protosLoaded = protoFiles.stream()
				.map(e -> configuration.inputDirectory.toURI().relativize(e.toURI()).getPath())
				.collect(Collectors.toList());

		if (configuration.includeValidationRules) {
			schemaLoader.addProto("validate/validate.proto");
		}

		for (String importRootFolder : configuration.customImportLocations) {
			schemaLoader.addSource(new File(configuration.basedir, importRootFolder).toPath());
		}

		schemaLoader.addSource(configuration.inputDirectory);

		for (Path s : schemaLoader.sources()) {
			LOGGER.debug("Linking proto from path " + s);
		}
		for (String s : schemaLoader.protos()) {
			LOGGER.debug("Linking proto " + s);
		}

		Schema schema = schemaLoader.load();

		IdentifierSet.Builder b = new IdentifierSet.Builder();
		b.exclude(configuration.excludes);
		b.include(configuration.includes);

		Schema prunedSchema = schema.prune(b.build());

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

}
