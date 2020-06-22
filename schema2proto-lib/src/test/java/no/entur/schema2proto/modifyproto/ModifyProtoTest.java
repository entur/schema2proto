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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.entur.schema2proto.AbstractMappingTest;
import no.entur.schema2proto.modifyproto.config.FieldOption;
import no.entur.schema2proto.modifyproto.config.MergeFrom;
import no.entur.schema2proto.modifyproto.config.ModifyProtoConfiguration;
import no.entur.schema2proto.modifyproto.config.NewField;

public class ModifyProtoTest extends AbstractMappingTest {

	@Test
	public void testRemoveIndependentMessageType() throws IOException, InvalidProtobufException {

		File expected = new File("src/test/resources/modify/expected/nopackagename").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/nopackagename").getCanonicalFile();

		List<String> excludes = new ArrayList<>();
		excludes.add("A");

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.excludes = excludes;
		modifyProto(configuration);
		compareExpectedAndGenerated(expected, "missing_a.proto", generatedRootFolder, "simple.proto");

	}

	@Test
	public void testWhitelistMessageType() throws IOException, InvalidProtobufException {

		File expected = new File("src/test/resources/modify/expected/nopackagename").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/nopackagename").getCanonicalFile();

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.includes = Collections.singletonList("A");
		modifyProto(configuration);

		compareExpectedAndGenerated(expected, "only_a.proto", generatedRootFolder, "simple.proto");

	}

	@Test
	public void testWhitelistMessageTypeWithIncludeBaseTypeOptionEnabled() throws IOException, InvalidProtobufException {

		File expected = new File("src/test/resources/modify/expected/xsdbasetype").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/xsdbasetype").getCanonicalFile();

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.includes = Collections.singletonList("B");
		configuration.includeBaseTypes = true;
		modifyProto(configuration);

		compareExpectedAndGenerated(expected, "enabled.proto", generatedRootFolder, "simple.proto");

	}

	@Test
	public void testWhitelistMessageTypeWithIncludeBaseTypeOptionDisabled() throws IOException, InvalidProtobufException {

		File expected = new File("src/test/resources/modify/expected/xsdbasetype").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/xsdbasetype").getCanonicalFile();

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.includes = Collections.singletonList("B");
		modifyProto(configuration);

		compareExpectedAndGenerated(expected, "disabled.proto", generatedRootFolder, "simple.proto");

	}

	@Test
	public void testRemoveFieldAndType() throws IOException, InvalidProtobufException {

		File expected = new File("src/test/resources/modify/expected/nopackagename").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/nopackagename").getCanonicalFile();

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.excludes = Collections.singletonList("LangType");

		modifyProto(configuration);
		compareExpectedAndGenerated(expected, "no_langtype.proto", generatedRootFolder, "simple.proto");
	}

	@Test
	public void removeEmptyFilesAndImports() throws IOException, InvalidProtobufException {
		File expected = new File("src/test/resources/modify/expected/emptyfile").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/emptyfile").getCanonicalFile();

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.excludes = Arrays.asList("package.ExcludeMessage", "package.ExcludeType");
		modifyProto(configuration);
		compareExpectedAndGenerated(expected, "package/no_exclusions.proto", generatedRootFolder, "package/importsexcluded.proto");

	}

	@Test
	public void testWhitelistMessageTypeAndDependency() throws IOException, InvalidProtobufException {

		File expected = new File("src/test/resources/modify/expected/nopackagename").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/nopackagename").getCanonicalFile();

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.includes = Collections.singletonList("B");
		modifyProto(configuration);

		compareExpectedAndGenerated(expected, "missing_a.proto", generatedRootFolder, "simple.proto");

	}

	@Test
	public void testInsidePackage() throws IOException, InvalidProtobufException {

		File expected = new File("src/test/resources/modify/expected/withpackagename").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/withpackagename").getCanonicalFile();

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.includes = Collections.singletonList("package.B");
		modifyProto(configuration);

		compareExpectedAndGenerated(expected, "package/insidepackage.proto", generatedRootFolder, "package/simple.proto");

	}

	@Test
	public void testAddField() throws IOException, InvalidProtobufException {

		File expected = new File("src/test/resources/modify/expected/nopackagename").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/nopackagename").getCanonicalFile();

		NewField newField = new NewField();
		newField.targetMessageType = "A";
		// newField.importProto = "importpackage/p.proto";
		newField.label = "repeated";
		newField.fieldNumber = 100;
		newField.name = "new_field";
		newField.type = "B";

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.newFields = Collections.singletonList(newField);
		modifyProto(configuration);

		compareExpectedAndGenerated(expected, "extrafield.proto", generatedRootFolder, "simple.proto");

	}

	@Test
	public void testAddFieldOption() throws IOException, InvalidProtobufException {

		File expected = new File("src/test/resources/modify/expected/nopackagename").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/nopackagename").getCanonicalFile();

		List<FieldOption> fieldOptions = new ArrayList<>();

		FieldOption fieldOption = new FieldOption();
		fieldOption.targetMessageType = "A";
		fieldOption.field = "response_timestamp";
		fieldOption.option = "[(validate.rules).uint64.gte = 20]";
		fieldOptions.add(fieldOption);

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.fieldOptions = Collections.singletonList(fieldOption);

		modifyProto(configuration);

		compareExpectedAndGenerated(expected, "addedFieldOption.proto", generatedRootFolder, "simple.proto");

	}

	@Test
	public void testAddEnumValue() throws IOException, InvalidProtobufException {

		File expected = new File("src/test/resources/modify/expected/nopackagename").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/nopackagename").getCanonicalFile();

		List<NewField> newFields = new ArrayList<>();

		NewField newField = new NewField();
		newField.targetMessageType = "A";
		// newField.importProto = "importpackage/p.proto";
		newField.label = "repeated";
		newField.fieldNumber = 100;
		newField.name = "new_field";
		newField.type = "B";
		newFields.add(newField);

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.newFields = Collections.singletonList(newField);
		modifyProto(configuration);

		compareExpectedAndGenerated(expected, "extrafield.proto", generatedRootFolder, "simple.proto");

	}

	@Test
	public void testMergeProto() throws IOException, InvalidProtobufException {

		File expected = new File("src/test/resources/modify/expected/nopackagename").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/nopackagename").getCanonicalFile();
		File mergefrom = new File("src/test/resources/modify/mergefrom/nopackagename").getCanonicalFile();

		MergeFrom m = new MergeFrom();
		m.sourceFolder = mergefrom;
		m.protoFile = "mergefrom.proto";

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.mergeFrom = Collections.singletonList(m);
		modifyProto(configuration);

		compareExpectedAndGenerated(expected, "mergefrom.proto", generatedRootFolder, "simple.proto");

	}
}
