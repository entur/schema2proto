package no.entur.schema2proto.wire;

/*-
 * #%L
 * schema2proto-wire
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.squareup.wire.Syntax;
import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.ProtoType;

public class MutableSchemaTest {

	@TempDir
	Path tempDir;

	private MutableSchema schema;

	@BeforeEach
	public void loadSchema() throws IOException {
		Files.createDirectories(tempDir.resolve("a"));
		Files.createDirectories(tempDir.resolve("b"));
		Files.writeString(tempDir.resolve("a/a.proto"), """
				syntax = "proto3";
				package a;
				import "b/b.proto";
				option java_package = "com.example.a";
				message Outer {
				  message Inner {
				    string text = 1;
				  }
				  Inner inner = 1;
				  repeated b.Other others = 2;
				  oneof choice {
				    Inner inner_choice = 3;
				    int64 number = 4;
				  }
				  reserved 10;
				}
				""");
		Files.writeString(tempDir.resolve("b/b.proto"), """
				syntax = "proto3";
				package b;
				message Other {
				  string id = 1;
				}
				enum Color {
				  COLOR_UNSPECIFIED = 0;
				}
				""");
		schema = WireSchemaLoader.loadMutable(List.of(tempDir), List.of());
	}

	@Test
	public void testFieldTypesAreResolved() {
		MutableMessageType outer = (MutableMessageType) schema.getType("a.Outer");

		assertEquals(ProtoType.get("a.Outer.Inner"), outer.field("inner").type());
		assertEquals("Inner", outer.field("inner").getElementType());
		assertEquals(ProtoType.get("b.Other"), outer.field("others").type());
		assertEquals(ProtoType.STRING, ((MutableMessageType) schema.getType("a.Outer.Inner")).field("text").type());
		assertEquals("a", outer.field("others").packageName());
	}

	@Test
	public void testOneOfFieldTypesAreResolved() {
		MutableMessageType outer = (MutableMessageType) schema.getType("a.Outer");

		assertEquals(ProtoType.get("a.Outer.Inner"), outer.field("inner_choice").type());
		assertEquals(ProtoType.INT64, outer.field("number").type());
	}

	@Test
	public void testTypeLookup() {
		MutableMessageType outer = (MutableMessageType) schema.getType(ProtoType.get("a.Outer"));

		assertSame(outer.nestedTypes().get(0), schema.getType("a.Outer.Inner"));
		assertTrue(schema.getType("b.Color") instanceof MutableEnumType);
		assertNull(schema.getType("string"));
		assertNull(schema.getType("a.Missing"));
	}

	@Test
	public void testProtoFileLookup() {
		MutableProtoFile file = schema.protoFile("a/a.proto");

		assertEquals("a", file.packageName());
		assertEquals(Syntax.PROTO_3, file.getSyntax());
		assertSame(schema.getType("a.Outer"), file.types().get(0));
		assertTrue(schema.protoFiles().contains(file));
		assertNull(schema.protoFile("missing.proto"));
		assertEquals("com.example.a", file.options().getOptionElements().get(0).getValue());
	}

	@Test
	public void testLoaderBuilder() throws IOException {
		MutableSchema onlyB = new MutableSchemaLoader().addSource(tempDir.toFile()).addProto("b/b.proto").load();

		assertTrue(onlyB.getType("b.Other") instanceof MutableMessageType);
		assertNull(onlyB.getType("a.Outer"));
		assertTrue(new MutableSchemaLoader().addSource(tempDir).load().getType("a.Outer") instanceof MutableMessageType);
	}

	@Test
	public void testLinkedSchemaIsExposed() {
		assertEquals("a.Outer", schema.schema().getType("a.Outer").getType().toString());
	}

	@Test
	public void testUpdateElementTypeClearsResolvedType() {
		MutableField field = ((MutableMessageType) schema.getType("a.Outer")).field("inner");

		field.updateElementType("string");

		assertNull(field.type());
	}

	@Test
	public void testLabelAccessors() {
		MutableMessageType outer = (MutableMessageType) schema.getType("a.Outer");

		assertTrue(outer.field("others").isRepeated());
		assertFalse(outer.field("inner").isRepeated());
		assertFalse(outer.field("inner").isRequired());
	}

	@Test
	public void testModifyAndSerialize() {
		MutableProtoFile file = schema.protoFile("b/b.proto");
		MutableMessageType other = (MutableMessageType) schema.getType("b.Other");

		other.addField(new MutableField(null, new Location("", "", 0, 0), Field.Label.REPEATED, "tags", null, 2, null, "string",
				new MutableOptions(MutableOptions.FIELD_OPTIONS, new ArrayList<>()), false, false));

		String serialized = file.toSchema();
		assertTrue(serialized.contains("repeated string tags = 2;"), serialized);
	}

	@Test
	public void testConstructorsKeepListsAsGiven() {
		List<MutableField> fields = new ArrayList<>();
		List<MutableType> types = new ArrayList<>();
		MutableMessageType message = new MutableMessageType(ProtoType.get("c.Message"), Location.get(""), "", "Message", fields, new ArrayList<>(),
				new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new MutableOptions(MutableOptions.MESSAGE_OPTIONS, new ArrayList<>()));
		MutableProtoFile file = new MutableProtoFile(Location.get("c/c.proto"), List.of(), List.of(), "c", types, List.of(), List.of(),
				new MutableOptions(MutableOptions.FILE_OPTIONS, new ArrayList<>()), Syntax.PROTO_3);

		fields.add(new MutableField(null, new Location("", "", 0, 0), null, "id", null, 1, null, "string",
				new MutableOptions(MutableOptions.FIELD_OPTIONS, new ArrayList<>()), false, false));
		types.add(message);

		assertEquals(1, message.fields().size());
		assertSame(message, file.types().get(0));
		assertTrue(file.toSchema().contains("string id = 1;"), file.toSchema());
	}
}
