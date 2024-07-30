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
package no.entur.schema2proto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.squareup.wire.schema.EnumConstant;
import com.squareup.wire.schema.EnumType;
import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.Options;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.ProtoMember;
import com.squareup.wire.schema.Rpc;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.SchemaLoader;
import com.squareup.wire.schema.Service;
import com.squareup.wire.schema.Type;

public class ProtoComparator {

	public static void compareProtoFiles(File expectedRootFolder, File expectedFile, File generatedRootFolder, File generatedFile) {
		try {
			ProtoFile expected = load(expectedRootFolder, expectedFile);
			ProtoFile generated = load(generatedRootFolder, generatedFile);

			assertEquals(expected.javaPackage(), generated.javaPackage(),
					"java package mismatch" + generateLocationInformation(expected.location(), generated.location()));
			assertEquals(expected.packageName(), generated.packageName(),
					"package name mismatch" + generateLocationInformation(expected.location(), generated.location()));
			// assertEquals(expected.name(), generated.name(), "name mismatch" + generateLocationInformation(expected.location(), generated.location()));

			compareOptions(expected.options(), generated.options());
			compareServices(expected, generated);
			compareTypes(expected, generated, expected.types(), generated.types());
		} catch (IOException e) {
			fail("Error loading protos for comparison", e);
		}

	}

	private static void compareServices(ProtoFile expected, ProtoFile generated) {

		Map<String, Service> expectedMap = new HashMap<>();
		Map<String, Service> generatedMap = new HashMap<>();

		for (Service t : expected.services()) {
			expectedMap.put(t.name(), t);
		}

		for (Service t : generated.services()) {
			generatedMap.put(t.name(), t);
		}

		Set<String> missing = new HashSet<>(expectedMap.keySet());
		missing.removeAll(generatedMap.keySet());
		assertTrue(missing.isEmpty(), "Services not found in generated proto file: " + missing);

		Set<String> unexpected = new HashSet<>(generatedMap.keySet());
		unexpected.removeAll(expectedMap.keySet());
		assertTrue(unexpected.isEmpty(), "Unexpected services found in generated proto file: " + unexpected);

		for (Entry<String, Service> expectedService : expectedMap.entrySet()) {
			Service generatedType = generatedMap.get(expectedService.getKey());
			compareService(expectedService.getValue(), generatedType);
		}
	}

	private static void compareService(Service expected, Service generated) {

		assertEquals(expected.documentation(), generated.documentation(),
				"Service documentation mismatch" + generateLocationInformation(expected.location(), generated.location()));
		assertEquals(expected.name(), generated.name(), "Service name mismatch" + generateLocationInformation(expected.location(), generated.location()));

		compareRpcs(expected, generated);

	}

	private static void compareRpcs(Service expected, Service generated) {
		Map<String, Rpc> expectedMap = new HashMap<>();
		Map<String, Rpc> generatedMap = new HashMap<>();

		for (Rpc t : expected.rpcs()) {
			expectedMap.put(t.name(), t);
		}

		for (Rpc t : generated.rpcs()) {
			generatedMap.put(t.name(), t);
		}

		Set<String> missing = new HashSet<>(expectedMap.keySet());
		missing.removeAll(generatedMap.keySet());
		assertTrue(missing.isEmpty(), "Services not found in generated proto file: " + missing);

		Set<String> unexpected = new HashSet<>(generatedMap.keySet());
		unexpected.removeAll(expectedMap.keySet());
		assertTrue(unexpected.isEmpty(), "Unexpected services found in generated proto file: " + unexpected);

		for (Entry<String, Rpc> expectedService : expectedMap.entrySet()) {
			Rpc generatedType = generatedMap.get(expectedService.getKey());
			compareRpc(expectedService.getValue(), generatedType);
		}
	}

	private static void compareRpc(Rpc expected, Rpc generated) {
		assertEquals(expected.documentation(), generated.documentation(),
				"Rpc call documentation mismatch" + generateLocationInformation(expected.location(), generated.location()));
		assertEquals(expected.name(), generated.name(), "Rpc call name mismatch" + generateLocationInformation(expected.location(), generated.location()));
		assertEquals(expected.requestStreaming(), generated.requestStreaming(),
				"Rpc call request streaming mismatch" + generateLocationInformation(expected.location(), generated.location()));
		assertEquals(expected.requestType(), generated.requestType(),
				"Rpc call request type mismatch" + generateLocationInformation(expected.location(), generated.location()));
		assertEquals(expected.responseStreaming(), generated.responseStreaming(),
				"Rpc call response streaming mismatch" + generateLocationInformation(expected.location(), generated.location()));
		assertEquals(expected.responseType(), generated.responseType(),
				"Rpc call response type mismatch" + generateLocationInformation(expected.location(), generated.location()));

		compareOptions(expected.options(), generated.options());
	}

	private static void compareOptions(Options expected, Options generated) {

		Set<ProtoMember> missing = new HashSet<>(expected.map().keySet());
		missing.removeAll(generated.map().keySet());
		assertTrue(missing.isEmpty(), "Options not found in generated proto file: " + missing);

		Set<ProtoMember> unexpected = new HashSet<>(generated.map().keySet());
		unexpected.removeAll(expected.map().keySet());
		assertTrue(unexpected.isEmpty(), "Unexpected options found in generated proto file: " + unexpected);

		for (Entry<ProtoMember, Object> expectedService : expected.map().entrySet()) {
			Object generatedType = generated.map().get(expectedService.getKey());
			assertEquals(expectedService.getValue(), generatedType, "Options value mismatch for " + expectedService.getKey());
		}
	}

	private static void compareTypes(ProtoFile expected, ProtoFile generated, List<Type> _expectedTypes, List<Type> _generatedTypes) {
		// Compare types
		Map<String, Type> expectedTypes = new HashMap<>();
		Map<String, Type> generatedTypes = new HashMap<>();

		for (Type t : _expectedTypes) {
			expectedTypes.put(t.type().simpleName(), t);
		}

		for (Type t : _generatedTypes) {

			generatedTypes.put(t.type().simpleName(), t);
		}

		Set<String> missingTypes = new HashSet<>(expectedTypes.keySet());
		missingTypes.removeAll(generatedTypes.keySet());
		assertTrue(missingTypes.isEmpty(), "Types not found in generated proto file: " + missingTypes + " generated file " + generated);

		Set<String> unexpectedTypes = new HashSet<>(generatedTypes.keySet());
		unexpectedTypes.removeAll(expectedTypes.keySet());
		assertTrue(unexpectedTypes.isEmpty(),
				"Unexpected types found in generated proto file but not in reference: " + unexpectedTypes + " generated file " + generated);

		for (Entry<String, Type> expectedType : expectedTypes.entrySet()) {

			Type generatedType = generatedTypes.get(expectedType.getKey());

			compareType(expectedType.getValue(), generatedType, expected, generated);

		}

	}

	private static void compareType(Type expected, Type generated, ProtoFile expectedFile, ProtoFile generatedFile) {

		assertEquals(expected.documentation(), generated.documentation(),
				"Type documentation mismatch" + generateLocationInformation(expected.location(), generated.location()));
		assertEquals(expected.type().toString(), generated.type().toString(),
				"Type information mismatch" + generateLocationInformation(expected.location(), generated.location()));

		assertEquals(expected.getClass(), generated.getClass());
		if (expected instanceof MessageType) {
			compareMessageType((MessageType) expected, (MessageType) generated);
		}
		if (expected instanceof EnumType) {
			compareEnumType((EnumType) expected, (EnumType) generated);
		}

		compareTypes(expectedFile, generatedFile, expected.nestedTypes(), generated.nestedTypes());

	}

	private static void compareMessageType(MessageType expectedType, MessageType generatedType) {
		compareFields(expectedType, generatedType);
	}

	private static void compareEnumType(EnumType expected, EnumType generated) {

		assertEquals(expected.documentation(), generated.documentation(),
				"Enum documentation mismatch" + generateLocationInformation(expected.location(), generated.location()));
		compareEnumConstants(expected.constants(), generated.constants());

	}

	private static void compareEnumConstants(List<EnumConstant> expected, List<EnumConstant> generated) {
		Map<String, EnumConstant> expectedMap = new HashMap<>();
		Map<String, EnumConstant> generatedMap = new HashMap<>();

		for (EnumConstant t : expected) {
			expectedMap.put(t.getName(), t);
		}

		for (EnumConstant t : generated) {
			generatedMap.put(t.getName(), t);
		}

		Set<String> missing = new HashSet<>(expectedMap.keySet());
		missing.removeAll(generatedMap.keySet());
		assertTrue(missing.isEmpty(), "Fields not found in generated proto file: " + missing);

		Set<String> unexpected = new HashSet<>(generatedMap.keySet());
		unexpected.removeAll(expectedMap.keySet());
		assertTrue(unexpected.isEmpty(), "Unexpected fields found in generated proto file but not in reference: " + unexpected);

		for (Entry<String, EnumConstant> expectedValue : expectedMap.entrySet()) {

			EnumConstant generatedValue = generatedMap.get(expectedValue.getKey());

			compareEnumConstant(expectedValue.getValue(), generatedValue);

		}

	}

	private static void compareEnumConstant(EnumConstant expected, EnumConstant generated) {
		assertEquals(expected.getDocumentation(), generated.getDocumentation(),
				"EnumConstant documentation mismatch" + generateLocationInformation(expected.getLocation(), generated.getLocation()));
		assertEquals(expected.getName(), generated.getName(),
				"EnumConstant name mismatch" + generateLocationInformation(expected.getLocation(), generated.getLocation()));
		assertEquals(expected.getTag(), generated.getTag(),
				"EnumConstant tag mismatch" + generateLocationInformation(expected.getLocation(), generated.getLocation()));
	}

	private static void compareFields(MessageType expected, MessageType generated) {

		Map<String, Field> expectedFields = new HashMap<>();
		Map<String, Field> generatedFields = new HashMap<>();

		for (Field t : expected.fields()) {
			expectedFields.put(t.name(), t);
		}

		for (Field t : generated.fields()) {
			generatedFields.put(t.name(), t);
		}

		Set<String> missingFields = new HashSet<>(expectedFields.keySet());
		missingFields.removeAll(generatedFields.keySet());
		assertTrue(missingFields.isEmpty(), "Fields not found in generated proto file: " + missingFields + ", type " + generated);

		Set<String> unexpectedFields = new HashSet<>(generatedFields.keySet());
		unexpectedFields.removeAll(expectedFields.keySet());
		assertTrue(unexpectedFields.isEmpty(),
				"Unexpected fields found in generated proto file but not in reference: " + unexpectedFields + ", type " + generated);

		for (Entry<String, Field> expectedField : expectedFields.entrySet()) {

			Field generatedField = generatedFields.get(expectedField.getKey());

			compareField(expectedField.getValue(), generatedField);

		}

	}

	private static void compareField(Field expected, Field generated) {
		assertEquals(expected.documentation(), generated.documentation(),
				"Field documentation mismatch " + expected.toString() + generateLocationInformation(expected.location(), generated.location()));
		assertEquals(expected.name(), generated.name(),
				"Field name mismatch " + expected.toString() + generateLocationInformation(expected.location(), generated.location()));
		assertEquals(expected.isRepeated(), generated.isRepeated(),
				"Field isRepeated mismatch " + expected.toString() + generateLocationInformation(expected.location(), generated.location()));
		assertEquals(expected.isDeprecated(), generated.isDeprecated(),
				"Field isDeprecated mismatch " + expected.toString() + generateLocationInformation(expected.location(), generated.location()));
		assertEquals(expected.isExtension(), generated.isExtension(),
				"Field isExtension mismatch " + expected.toString() + generateLocationInformation(expected.location(), generated.location()));
		assertEquals(expected.isOptional(), generated.isOptional(),
				"Field isOptional mismatch " + expected.toString() + generateLocationInformation(expected.location(), generated.location()));
		assertEquals(expected.isPacked(), generated.isPacked(),
				"Field isPacked mismatch " + expected.toString() + generateLocationInformation(expected.location(), generated.location()));
		assertEquals(expected.isRequired(), generated.isRequired(),
				"Field isRequired mismatch " + expected.toString() + generateLocationInformation(expected.location(), generated.location()));
		assertEquals(expected.isRedacted(), generated.isRedacted(),
				"Field isRedacted mismatch " + expected.toString() + generateLocationInformation(expected.location(), generated.location()));
		assertEquals(expected.tag(), generated.tag(),
				"Field tag mismatch " + expected.toString() + generateLocationInformation(expected.location(), generated.location()));
		assertEquals(expected.type().simpleName(), generated.type().simpleName(),
				"Field type mismatch " + expected.toString() + generateLocationInformation(expected.location(), generated.location()));
	}

	private static ProtoFile load(File rootFolder, File protoFilename) throws IOException {
		SchemaLoader schemaLoader = new SchemaLoader();
		schemaLoader.addSource(rootFolder);
		schemaLoader.addProto(protoFilename.getPath());
		Schema schema = schemaLoader.load();
		ProtoFile protofile = schema.protoFile(protoFilename.getPath());
		return protofile;
	}

	private static String generateLocationInformation(Location expected, Location generated) {
		return " at generated file " + generated + ". Expected result at " + expected;
	}

}
