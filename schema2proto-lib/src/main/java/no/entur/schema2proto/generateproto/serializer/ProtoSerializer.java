package no.entur.schema2proto.generateproto.serializer;

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

import static no.entur.schema2proto.generateproto.serializer.CommonUtils.getPathFromPackageName;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.SchemaLoader;

import no.entur.schema2proto.InvalidConfigurationException;
import no.entur.schema2proto.generateproto.*;

public class ProtoSerializer {
	private static final Logger LOGGER = LoggerFactory.getLogger(SchemaParser.class);
	private Schema2ProtoConfiguration configuration;

	private TypeAndNameMapper typeAndFieldNameMapper;

	private Set<String> basicTypes = new HashSet<>();

	private Stream<Processor> processors;

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

		processors = Stream.of(new ReplaceGeneratedSuffix(), new RemoveUnwantedFields(typeAndFieldNameMapper), new UppercaseMessageNames(),
				new AddConfigurationSpecifiedOptions(configuration), new ComputeFilenames(), new TranslateTypes(typeAndFieldNameMapper, basicTypes),
				new ReplaceTypes(typeAndFieldNameMapper), new ComputeLocalImports(), new AddConfigurationSpecifiedImports(configuration),
				new ResolveRecursiveImports(), new HandleFieldNameCaseInsensitives(), new TranslateFieldNames(typeAndFieldNameMapper),
				new MoveFieldPackageNameToFieldTypeName(), new AddLeadingPeriodToElementType(), new UnderscoreFieldNames(),
				new EscapeReservedJavaKeywords(typeAndFieldNameMapper), new UpdateEnumValues());
	}

	public void serialize(Map<String, ProtoFile> packageToProtoFileMap) throws InvalidXSDException, IOException {
		processors.forEach(processor -> processor.process(packageToProtoFileMap));
		writeFiles(packageToProtoFileMap);
		parseAndVerifyWrittenFiles();
	}

	private void writeFiles(Map<String, ProtoFile> packageToProtoFileMap) throws InvalidXSDException, IOException {
		if (configuration.outputFilename != null) {
			if (packageToProtoFileMap.size() > 1) {
				LOGGER.error("Source schema contains multiple namespaces but specifies a single output file");
				throw new InvalidXSDException();
			} else {
				ProtoFile protoFile = packageToProtoFileMap.entrySet().iterator().next().getValue();
				File destFolder = createPackageFolderStructure(configuration.outputDirectory, protoFile.packageName());

				File outputFile = new File(destFolder, configuration.outputFilename.toLowerCase());
				writeFile(protoFile, outputFile);
			}
		} else {
			for (Entry<String, ProtoFile> entry : packageToProtoFileMap.entrySet()) {
				ProtoFile protoFile = entry.getValue();
				File destFolder = createPackageFolderStructure(configuration.outputDirectory, protoFile.packageName());
				File outputFile = new File(destFolder, protoFile.location().getPath().toLowerCase());

				writeFile(protoFile, outputFile);
			}
		}
	}

	private void writeFile(ProtoFile protoFile, File outputFile) throws IOException {
		try (Writer writer = new FileWriter(outputFile)) {
			writer.write(protoFile.toSchema());
		}
	}

	private File createPackageFolderStructure(File outputDirectory, String packageName) {
		String folderSubstructure = getPathFromPackageName(packageName);
		File dstFolder = new File(outputDirectory, folderSubstructure);
		dstFolder.mkdirs();
		return dstFolder;
	}

	private void parseAndVerifyWrittenFiles() throws IOException {
		SchemaLoader schemaLoader = new SchemaLoader();
		try {
			if (configuration.includeValidationRules) {
				schemaLoader.addProto("validate/validate.proto");
			}
			for (String importRootFolder : configuration.customImportLocations) {
				schemaLoader.addSource(new File(importRootFolder).toPath());
			}
			schemaLoader.addSource(configuration.outputDirectory);
			schemaLoader.load();
		} catch (IOException e) {
			LOGGER.error("Parsing of written output failed, the proto files are not valid", e);
			throw e;
		}
	}
}
