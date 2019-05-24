package no.entur.schema2proto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.squareup.wire.schema.Options;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.ProtoMember;
import com.squareup.wire.schema.Rpc;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.SchemaLoader;
import com.squareup.wire.schema.Service;
import com.squareup.wire.schema.Type;

public class ProtoComparator {

	public static void compareProtoFiles(File expected, File actual) {
		try {
			ProtoFile expectedProto = load(expected.getParentFile(), expected.getName());
			ProtoFile generatedProto = load(actual.getParentFile(), actual.getName());

			assertEquals(expectedProto.javaPackage(), generatedProto.javaPackage());
			assertEquals(expectedProto.packageName(), generatedProto.packageName());
			assertEquals(expectedProto.name(), generatedProto.name());

			compareOptions(expectedProto.options(), generatedProto.options());
			compareServices(expectedProto.services(), generatedProto.services());
			compareTypes(expectedProto.types(), generatedProto.types());
		} catch (IOException e) {
			fail("Error loading protos for comparison", e);
		}

	}

	private static void compareServices(ImmutableList<Service> expected, ImmutableList<Service> generated) {

		Map<String, Service> expectedMap = new HashMap<>();
		Map<String, Service> generatedMap = new HashMap<>();

		for (Service t : expected) {
			expectedMap.put(t.name(), t);
		}

		for (Service t : generated) {
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

		assertEquals(expected.documentation(), generated.documentation(), "Service documentation mismatch");
		assertEquals(expected.name(), generated.name(), "Service name mismatch");

		compareRpcs(expected.rpcs(), generated.rpcs());

	}

	private static void compareRpcs(ImmutableList<Rpc> expected, ImmutableList<Rpc> generated) {
		Map<String, Rpc> expectedMap = new HashMap<>();
		Map<String, Rpc> generatedMap = new HashMap<>();

		for (Rpc t : expected) {
			expectedMap.put(t.name(), t);
		}

		for (Rpc t : generated) {
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
		assertEquals(expected.documentation(), generated.documentation(), "Rpc call documentation mismatch");
		assertEquals(expected.name(), generated.name(), "Rpc call name mismatch");
		assertEquals(expected.requestStreaming(), generated.requestStreaming(), "Rpc call request streaming mismatch");
		assertEquals(expected.requestType(), generated.requestType(), "Rpc call request type mismatch");
		assertEquals(expected.responseStreaming(), generated.responseStreaming(), "Rpc call response streaming mismatch");
		assertEquals(expected.responseType(), generated.responseType(), "Rpc call response type mismatch");

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

	private static void compareTypes(ImmutableList<Type> expected, ImmutableList<Type> generated) {
		// Compare types
		Map<String, Type> expectedTypes = new HashMap<>();
		Map<String, Type> generatedTypes = new HashMap<>();

		for (Type t : expected) {
			expectedTypes.put(t.type().simpleName(), t);
		}

		for (Type t : generated) {
			generatedTypes.put(t.type().simpleName(), t);
		}

		Set<String> missingTypes = new HashSet<>(expectedTypes.keySet());
		missingTypes.removeAll(generatedTypes.keySet());
		assertTrue(missingTypes.isEmpty(), "Types not found in generated proto file: " + missingTypes);

		Set<String> unexpectedTypes = new HashSet<>(generatedTypes.keySet());
		unexpectedTypes.removeAll(expectedTypes.keySet());
		assertTrue(unexpectedTypes.isEmpty(), "Unexpected types found in generated proto file: " + unexpectedTypes);

		for (Entry<String, Type> expectedType : expectedTypes.entrySet()) {

			Type generatedType = generatedTypes.get(expectedType.getKey());

			compareType(expectedType.getValue(), generatedType);

		}

	}

	private static void compareType(Type expectedType, Type generatedType) {

		assertEquals(expectedType.documentation(), generatedType.documentation(), "Type documentation mismatch");
		assertEquals(expectedType.type().toString(), generatedType.type().toString(), "Type information mismatch");
		compareTypes(expectedType.nestedTypes(), generatedType.nestedTypes());

	}

	private static ProtoFile load(File folder, String protoFilename) throws IOException {
		SchemaLoader schemaLoader = new SchemaLoader();
		schemaLoader.addProto(protoFilename);
		schemaLoader.addSource(folder);
		Schema schema = schemaLoader.load();
		ProtoFile protofile = schema.protoFile(protoFilename);
		return protofile;
	}

}
