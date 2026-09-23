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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.entur.schema2proto.AbstractMappingTest;
import no.entur.schema2proto.InvalidConfigurationException;
import no.entur.schema2proto.modifyproto.config.FieldOption;
import no.entur.schema2proto.modifyproto.config.MergeFrom;
import no.entur.schema2proto.modifyproto.config.ModifyField;
import no.entur.schema2proto.modifyproto.config.ModifyProtoConfiguration;
import no.entur.schema2proto.modifyproto.config.NewEnumConstant;
import no.entur.schema2proto.modifyproto.config.NewField;

public class ModifyProtoTest extends AbstractMappingTest {

	@Test
	public void testRemoveIndependentMessageType() throws IOException, InvalidProtobufException, InvalidConfigurationException {

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
	public void testWhitelistMessageType() throws IOException, InvalidProtobufException, InvalidConfigurationException {

		File expected = new File("src/test/resources/modify/expected/nopackagename").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/nopackagename").getCanonicalFile();

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.includes = Collections.singletonList("A");
		modifyProto(configuration);

		compareExpectedAndGenerated(expected, "only_a.proto", generatedRootFolder, "simple.proto");

	}

	@Test
	public void testWhitelistMessageTypeWithIncludeBaseTypeOptionEnabled() throws IOException, InvalidProtobufException, InvalidConfigurationException {

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
	public void testWhitelistMessageTypeWithIncludeBaseTypeOptionDisabled() throws IOException, InvalidProtobufException, InvalidConfigurationException {

		File expected = new File("src/test/resources/modify/expected/xsdbasetype").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/xsdbasetype").getCanonicalFile();

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.includes = Collections.singletonList("B");
		modifyProto(configuration);

		compareExpectedAndGenerated(expected, "disabled.proto", generatedRootFolder, "simple.proto");

	}

	@Test
	public void testIncludeAndExcludeOverlapWithBaseTypes() throws IOException, InvalidProtobufException, InvalidConfigurationException {
		// Regression: when followOneMoreLevel adds a base type ("A", referenced by B's xsd.base_type) that is also excluded,
		// the identifier ends up in both roots and prunes. The vendored IdentifierSet tolerated this (excludes win); stock
		// wire's PruningRules rejects the overlap. ModifyProto must drop the overlap from the roots rather than fail.
		File source = new File("src/test/resources/modify/input/xsdbasetype").getCanonicalFile();

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.includes = Collections.singletonList("B");
		configuration.excludes = Collections.singletonList("A");
		configuration.includeBaseTypes = true;

		modifyProto(configuration);

		assertTrue(new File(generatedRootFolder, "simple.proto").exists(), "Expected proto to be generated without a roots/prunes overlap failure");
	}

	@Test
	public void testExcludedTypeBeatsMoreSpecificIncludedMember() throws IOException, InvalidProtobufException, InvalidConfigurationException {
		// The vendored IdentifierSet let an exclude win over an include at any level, while stock wire's PruningRules lets the more precise of the two win.
		// An excluded type must therefore still prune a member of it that was included by name.
		File source = new File("src/test/resources/modify/input/withpackagename").getCanonicalFile();

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.includes = Arrays.asList("package.A", "package.B#lang");
		configuration.excludes = Collections.singletonList("package.B");

		modifyProto(configuration);

		String generated = Files.readString(new File(generatedRootFolder, "package/simple.proto").toPath());
		assertTrue(generated.contains("message A"), generated);
		assertFalse(generated.contains("message B"), generated);
	}

	@Test
	public void testExcludedPackageBeatsIncludedTypeInIt() throws IOException, InvalidProtobufException, InvalidConfigurationException {
		// Same precedence rule one level up: a wildcard exclude covers an included type inside it. With every include excluded nothing is rooted, which is not
		// the same as the empty include list that means "root everything".
		File source = new File("src/test/resources/modify/input/withpackagename").getCanonicalFile();

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.includes = Collections.singletonList("package.LangType");
		configuration.excludes = Collections.singletonList("package.*");

		modifyProto(configuration);

		assertFalse(new File(generatedRootFolder, "package/simple.proto").exists(), "Expected nothing to be included, not the whole schema");
	}

	@Test
	public void testDuplicateOptionDefinitionAcrossSourceRootsIsDeduplicated() throws IOException, InvalidProtobufException, InvalidConfigurationException {
		// Regression: the xsd.base_type extension definition (xsd/xsd.proto) is commonly present in more than one source root
		// (e.g. unpacked proto deps and the input dir). Loading it from each root would define the extension twice, which stock
		// wire rejects ("multiple fields share tag/name"). The loader must deduplicate protos by path (first root wins).
		File source = new File("src/test/resources/modify/input/xsdbasetype").getCanonicalFile();

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		// A second source root that also contains xsd/xsd.proto (mirrors unpacked proto deps alongside the input dir).
		configuration.customImportLocations = Collections.singletonList("src/test/resources/modify/dupimport");
		configuration.includes = Collections.singletonList("B");
		configuration.includeBaseTypes = true;

		modifyProto(configuration);

		assertTrue(new File(generatedRootFolder, "simple.proto").exists(), "Expected proto to be generated despite duplicate xsd.proto across source roots");
	}

	@Test
	public void testServicesAndRpcsArePreserved() throws IOException, InvalidProtobufException, InvalidConfigurationException {
		// Regression: gRPC service/RPC declarations on existing protos must survive the modify round-trip (the builder model
		// previously dropped services, producing "missing RPC" protolock conflicts downstream).
		File expected = new File("src/test/resources/modify/expected/withservice").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/withservice").getCanonicalFile();

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;

		modifyProto(configuration);

		compareExpectedAndGenerated(expected, "svc.proto", generatedRootFolder, "svc.proto");
	}

	@Test
	public void testServiceOnlyFileIsPreserved() throws IOException, InvalidProtobufException, InvalidConfigurationException {
		// A file declaring only a service (its messages imported from elsewhere) is not empty and must still be written
		File expected = new File("src/test/resources/modify/expected/serviceonly").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/serviceonly").getCanonicalFile();

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;

		modifyProto(configuration);

		assertTrue(new File(generatedRootFolder, "svc/service.proto").exists(), "Expected service-only proto to be generated");
		compareExpectedAndGenerated(expected, "svc/service.proto", generatedRootFolder, "svc/service.proto");
	}

	@Test
	public void testRemoveFieldAndType() throws IOException, InvalidProtobufException, InvalidConfigurationException {

		File expected = new File("src/test/resources/modify/expected/nopackagename").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/nopackagename").getCanonicalFile();

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.excludes = Collections.singletonList("LangType");

		modifyProto(configuration);
		compareExpectedAndGenerated(expected, "no_langtype.proto", generatedRootFolder, "simple.proto");
	}

	@Test
	public void removeEmptyFilesAndImports() throws IOException, InvalidProtobufException, InvalidConfigurationException {
		File expected = new File("src/test/resources/modify/expected/emptyfile").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/emptyfile").getCanonicalFile();

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.excludes = Arrays.asList("package.ExcludeMessage", "package.ExcludeType");
		modifyProto(configuration);
		compareExpectedAndGenerated(expected, "package/no_exclusions.proto", generatedRootFolder, "package/importsexcluded.proto");

	}

	@Test
	public void testWhitelistMessageTypeAndDependency() throws IOException, InvalidProtobufException, InvalidConfigurationException {

		File expected = new File("src/test/resources/modify/expected/nopackagename").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/nopackagename").getCanonicalFile();

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.includes = Collections.singletonList("B");
		modifyProto(configuration);

		compareExpectedAndGenerated(expected, "missing_a.proto", generatedRootFolder, "simple.proto");

	}

	@Test
	public void testInsidePackage() throws IOException, InvalidProtobufException, InvalidConfigurationException {

		File expected = new File("src/test/resources/modify/expected/withpackagename").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/withpackagename").getCanonicalFile();

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.includes = Collections.singletonList("package.B");
		modifyProto(configuration);

		compareExpectedAndGenerated(expected, "package/insidepackage.proto", generatedRootFolder, "package/simple.proto");

	}

	@Test
	public void testAddField() throws IOException, InvalidProtobufException, InvalidConfigurationException {

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
	public void testModifyField() throws IOException, InvalidProtobufException, InvalidConfigurationException {
		File expected = new File("src/test/resources/modify/expected/nopackagename").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/nopackagename").getCanonicalFile();

		ModifyField modifyField = new ModifyField();
		modifyField.targetMessageType = "A";
		modifyField.field = "response_timestamp";
		modifyField.documentationPattern = "(^.*$)";
		modifyField.documentation = "[Additional documentation] $1";

		ModifyField modifyField2 = new ModifyField();
		modifyField2.targetMessageType = "B";
		modifyField2.field = "value";
		modifyField2.documentationPattern = null;
		modifyField2.documentation = "Whole documentation replaced";

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.modifyFields = Arrays.asList(modifyField, modifyField2);
		modifyProto(configuration);

		compareExpectedAndGenerated(expected, "modifyfield.proto", generatedRootFolder, "simple.proto");
	}

	@Test
	public void testAddFieldOption() throws IOException, InvalidProtobufException, InvalidConfigurationException {

		File expected = new File("src/test/resources/modify/expected/nopackagename").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/nopackagename").getCanonicalFile();

		List<FieldOption> fieldOptions = new ArrayList<>();

		FieldOption fieldOption = new FieldOption();
		fieldOption.targetMessageType = "A";
		fieldOption.field = "response_timestamp";
		fieldOption.option = "[(buf.validate.field).uint64.gte = 20]";
		fieldOptions.add(fieldOption);

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.fieldOptions = Collections.singletonList(fieldOption);

		modifyProto(configuration);

		compareExpectedAndGenerated(expected, "addedFieldOption.proto", generatedRootFolder, "simple.proto");

	}

	@Test
	public void testAddedAggregateFieldOptionKeepsEscaping() throws IOException, InvalidProtobufException, InvalidConfigurationException {
		// Wire's parser unescapes strings but does not escape them again inside an aggregate option, so "\\d" would be emitted as "\d",
		// which protoc rejects. The comparator reparses both sides and cannot see the difference, so check the written text.
		File source = new File("src/test/resources/modify/input/nopackagename").getCanonicalFile();

		FieldOption fieldOption = new FieldOption();
		fieldOption.targetMessageType = "A";
		fieldOption.field = "response_timestamp";
		fieldOption.option = "[(buf.validate.field).string = { pattern: \"^\\\\d+$\" }]";

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.fieldOptions = Collections.singletonList(fieldOption);

		modifyProto(configuration);

		String generated = Files.readString(new File(generatedRootFolder, "simple.proto").toPath());
		assertTrue(generated.contains("pattern: \"^\\\\d+$\""), "Expected escaping to survive in:\n" + generated);
	}

	@Test
	public void testAddEnumValue() throws IOException, InvalidProtobufException, InvalidConfigurationException {

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
	public void testAddFieldAllowIfReserved() throws IOException, InvalidProtobufException, InvalidConfigurationException {
		// Add a field with same name/number as a reserved field. Should allow and remove reserved declarations.
		File expected = new File("src/test/resources/modify/expected/reserved").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/reserved").getCanonicalFile();

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		NewField newField = new NewField();
		newField.targetMessageType = "A";
		newField.fieldNumber = 100;
		newField.name = "reserved_field";
		newField.type = "string";
		newField.allowIfReserved = true;

		configuration.newFields = Collections.singletonList(newField);
		modifyProto(configuration);

		compareExpectedAndGenerated(expected, "addreservedfield.proto", generatedRootFolder, "addreservedfield.proto");
	}

	@Test
	public void testAddFieldAllowIfReserved_whenMultipleFieldsPerReservationDeclaration_thenRemoveOnlyReappearing()
			throws IOException, InvalidProtobufException, InvalidConfigurationException {
		// Add a field with same name/number as a reserved field. Should allow remove field number and name from reserved declarations, but keep other reserved
		// fields in the same declaration.
		File expected = new File("src/test/resources/modify/expected/reserved_multiple").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/reserved_multiple").getCanonicalFile();

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		NewField newField = new NewField();
		newField.targetMessageType = "A";
		newField.fieldNumber = 100;
		newField.name = "reserved_field";
		newField.type = "string";
		newField.allowIfReserved = true;

		configuration.newFields = Collections.singletonList(newField);
		modifyProto(configuration);

		compareExpectedAndGenerated(expected, "addreservedfield_multiple.proto", generatedRootFolder, "addreservedfield_multiple.proto");
	}

	@Test
	public void testAddFieldAllowIfReserved_whenTagIsInsideReservedRange_thenSplitRangeAroundIt()
			throws IOException, InvalidProtobufException, InvalidConfigurationException {
		// Taking a tag back from inside a reserved range has to split the range around it; dropping only values equal to the tag leaves the range
		// covering it, and wire's linker then reports "tag 150 is reserved" when the generated proto is loaded back. ProtoComparator does not compare
		// reserved declarations, so the surviving halves of the range are asserted on the emitted text directly.
		File expected = new File("src/test/resources/modify/expected/reserved_range").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/reserved_range").getCanonicalFile();

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		NewField newField = new NewField();
		newField.targetMessageType = "A";
		newField.fieldNumber = 150;
		newField.name = "reserved_field";
		newField.type = "string";
		newField.allowIfReserved = true;

		configuration.newFields = Collections.singletonList(newField);
		modifyProto(configuration);

		String generated = java.nio.file.Files.readString(new File(generatedRootFolder, "addreservedfield_range.proto").toPath());
		assertTrue(generated.contains("reserved 2, 100 to 149, 151 to 199;"), generated);

		compareExpectedAndGenerated(expected, "addreservedfield_range.proto", generatedRootFolder, "addreservedfield_range.proto");
	}

	@Test
	public void testAddFieldAllowIfReservedNameOnly() throws IOException, InvalidProtobufException, InvalidConfigurationException {
		// Add a field with same name as a reserved field.
		File expected = new File("src/test/resources/modify/expected/reserved").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/reserved").getCanonicalFile();

		NewField reappearedReservedField = new NewField();
		reappearedReservedField.targetMessageType = "A";
		reappearedReservedField.fieldNumber = 200;
		reappearedReservedField.name = "reserved_field";
		reappearedReservedField.type = "string";
		reappearedReservedField.allowIfReserved = true;

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.newFields = Collections.singletonList(reappearedReservedField);
		modifyProto(configuration);

		compareExpectedAndGenerated(expected, "addreservedfield_name_only.proto", generatedRootFolder, "addreservedfield.proto");
	}

	@Test
	public void testAddFieldAllowIfReservedIdOnly() throws IOException, InvalidProtobufException, InvalidConfigurationException {
		// Add a field with same number as a reserved field, but a new name
		File expected = new File("src/test/resources/modify/expected/reserved").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/reserved").getCanonicalFile();

		NewField reappearedReservedField = new NewField();
		reappearedReservedField.targetMessageType = "A";
		reappearedReservedField.fieldNumber = 100;
		reappearedReservedField.name = "new_name";
		reappearedReservedField.type = "string";
		reappearedReservedField.allowIfReserved = true;

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.newFields = Collections.singletonList(reappearedReservedField);
		modifyProto(configuration);

		compareExpectedAndGenerated(expected, "addreservedfield_id_only.proto", generatedRootFolder, "addreservedfield.proto");
	}

	@Test
	public void testAddFieldReservedThrowsException() throws IOException {
		// Add a field with both the same name and number as a reserved field
		File source = new File("src/test/resources/modify/input/reserved").getCanonicalFile();

		NewField reappearedReservedField = new NewField();
		reappearedReservedField.targetMessageType = "A";
		reappearedReservedField.fieldNumber = 100;
		reappearedReservedField.name = "reserved_field";
		reappearedReservedField.type = "string";
		// allowIfReserved defaults to false

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.newFields = Collections.singletonList(reappearedReservedField);

		assertThrows(InvalidProtobufException.class, () -> modifyProto(configuration));

	}

	@Test
	public void testAddEnumConstantAllowIfReserved_whenTagIsInsideReservedRange_thenSplitRangeAroundIt()
			throws IOException, InvalidProtobufException, InvalidConfigurationException {
		// Add an enum constant whose name and number are both reserved, the number inside a reserved range. The name reservation is dropped and the range is
		// split around the number. ProtoComparator does not compare reserved declarations, so the reservations are asserted on the emitted text directly.
		File expected = new File("src/test/resources/modify/expected/reserved_enum").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/reserved_enum").getCanonicalFile();

		NewEnumConstant newEnumConstant = new NewEnumConstant();
		newEnumConstant.targetEnumType = "A";
		newEnumConstant.fieldNumber = 12;
		newEnumConstant.name = "A_RESERVED";
		newEnumConstant.allowIfReserved = true;

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.newEnumConstants = Collections.singletonList(newEnumConstant);
		modifyProto(configuration);

		String generated = Files.readString(new File(generatedRootFolder, "addreservedenumconstant.proto").toPath());
		assertTrue(generated.contains("reserved 2, 9 to 11, 13 to 16;"), generated);
		assertFalse(generated.contains("\"A_RESERVED\""), generated);

		compareExpectedAndGenerated(expected, "addreservedenumconstant.proto", generatedRootFolder, "addreservedenumconstant.proto");
	}

	@Test
	public void testAddEnumConstantReservedThrowsException() throws IOException {
		// Add an enum constant with a reserved name and number
		File source = new File("src/test/resources/modify/input/reserved_enum").getCanonicalFile();

		NewEnumConstant newEnumConstant = new NewEnumConstant();
		newEnumConstant.targetEnumType = "A";
		newEnumConstant.fieldNumber = 12;
		newEnumConstant.name = "A_RESERVED";
		// allowIfReserved defaults to false

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.newEnumConstants = Collections.singletonList(newEnumConstant);

		assertThrows(InvalidProtobufException.class, () -> modifyProto(configuration));
	}

	@Test
	public void testMergeProto() throws IOException, InvalidProtobufException, InvalidConfigurationException {

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

	@Test
	public void removeFieldKeepOptions() throws IOException, InvalidProtobufException, InvalidConfigurationException {

		File expected = new File("src/test/resources/modify/expected/withoptions").getCanonicalFile();
		File source = new File("src/test/resources/modify/input/withoptions").getCanonicalFile();

		ModifyProtoConfiguration configuration = new ModifyProtoConfiguration();
		configuration.inputDirectory = source;
		configuration.excludes = Collections.singletonList("LangType");

		modifyProto(configuration);
		compareExpectedAndGenerated(expected, "no_langtype.proto", generatedRootFolder, "simple.proto");
	}
}
