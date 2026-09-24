package no.entur.schema2proto.wire;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.squareup.wire.Syntax;
import com.squareup.wire.schema.Extend;
import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.Options;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.ProtoType;
import com.squareup.wire.schema.internal.parser.OptionElement;
import com.squareup.wire.schema.internal.parser.ProtoParser;

public class SerializationTest {

	/** Builds a proto file through schema2proto's mutable builder model and verifies it serializes via stock wire. */
	@Test
	public void testBuildProtoFile() {

		MutableProtoFile f = new MutableProtoFile(Syntax.PROTO_3, "default");

		Location valueLocation = new Location("base", "file", 1, 2);
		List<MutableEnumConstant> values = new ArrayList<>();
		values.add(new MutableEnumConstant(valueLocation, "VAL1", 1, "doc", new MutableOptions(MutableOptions.ENUM_VALUE_OPTIONS, new ArrayList<>())));
		f.types()
				.add(new MutableEnumType(ProtoType.get("Name"), valueLocation, "doc", "Name", values, new ArrayList<>(),
						new MutableOptions(MutableOptions.ENUM_OPTIONS, new ArrayList<>())));

		Location messageLocation = new Location("base", "path", 1, 1);
		f.types()
				.add(new MutableMessageType(ProtoType.get("messagename"), messageLocation, "doc", "messagename",
						new MutableOptions(MutableOptions.MESSAGE_OPTIONS, new ArrayList<>())));

		String schema = f.toSchema();
		assertNotNull(schema);
	}

	/**
	 * Verifies that converting an existing proto into the mutable model and back does not drop declarations schema2proto never touches: weak imports, message
	 * level extend declarations, extension ranges and explicit json_name.
	 */
	@Test
	public void testRoundTripKeepsWeakImportsExtendsExtensionRangesAndJsonName() {
		String source = "syntax = \"proto2\";\n" + "package test;\n" + "\n" + "import \"other.proto\";\n" + "import public \"pub.proto\";\n"
				+ "import weak \"legacy.proto\";\n" + "\n" + "message Wrapped {\n" + "  optional string name = 1;\n"
				+ "  optional string some_name = 2 [json_name = \"someName\"];\n" + "\n" + "  extensions 100 to 199;\n" + "\n" + "  extend Wrapped {\n"
				+ "    optional int32 extra = 100;\n" + "  }\n" + "\n" + "  message Nested {\n" + "    extensions 200 to 299;\n" + "  }\n" + "}\n";

		ProtoFile protoFile = ProtoFile.Companion.get(ProtoParser.Companion.parse(Location.get("roundtrip.proto"), source));

		String schema = WireBuilders.fromProtoFile(protoFile).toSchema();

		assertTrue(schema.contains("import weak \"legacy.proto\";"), schema);
		assertTrue(schema.contains("import public \"pub.proto\";"), schema);
		assertTrue(schema.contains("extensions 100 to 199;"), schema);
		assertTrue(schema.contains("extensions 200 to 299;"), schema);
		assertTrue(schema.contains("extend Wrapped {"), schema);
		assertTrue(schema.contains("optional int32 extra = 100;"), schema);
		assertTrue(schema.contains("[json_name = \"someName\"]"), schema);
	}

	/**
	 * Pins down the guarantee that makes the modify path safe: for a file nothing has edited, converting into the mutable model and back produces exactly the
	 * schema stock wire itself would emit. Each node renders itself by copying the element it was built from, so a declaration survives whether or not the
	 * mutable model has a field for it. Before that, anything unrepresented was replaced by an empty default and silently vanished — which is how weak imports,
	 * nested extends, extension ranges, explicit json names and services were each lost in turn.
	 *
	 * <p>
	 * The oneOf is declared last on purpose. Wire emits a message's fields sorted by source location and opens the oneOf block where its first field falls,
	 * while schema2proto deliberately emits declared fields first and oneOf fields after them, reproducing the vendored serializer's order. The two agree only
	 * when the source already declares the oneOf last; that difference is about field order, not fidelity, and is not what this test is pinning down.
	 */
	@Test
	public void testUnmodifiedFileRoundTripsToTheSchemaStockWireEmits() {
		String source = """
				syntax = "proto2";
				package fidelity;

				import "common.proto";
				import public "shared.proto";
				import weak "legacy.proto";

				option java_package = "no.entur.fidelity";

				enum Kind {
				  option allow_alias = true;
				  reserved 5, 20 to 29;
				  reserved "OLD_KIND";
				  UNKNOWN = 0;
				  FIRST = 1 [deprecated = true];
				}

				message Envelope {
				  option (xsd.base_type) = "EnvelopeType";
				  reserved 9;
				  reserved "old_field";

				  required string id = 1;
				  optional int32 count = 2 [default = 3];
				  optional string some_name = 3 [json_name = "someName"];
				  repeated Kind kinds = 4 [packed = true];

				  oneof payload {
				    string text = 7;
				    bytes blob = 8;
				  }

				  extend Envelope {
				    optional int32 extra = 100;
				  }

				  extensions 100 to 199;

				  message Nested {
				    optional string note = 1;
				  }
				}

				extend google.protobuf.MessageOptions {
				  optional string base_type = 1101;
				}

				service EnvelopeService {
				  option deprecated = true;

				  rpc Send (Envelope) returns (Envelope) {
				    option idempotency_level = NO_SIDE_EFFECTS;
				  }
				}
				""";

		ProtoFile protoFile = ProtoFile.Companion.get(ProtoParser.Companion.parse(Location.get("fidelity.proto"), source));

		assertEquals(protoFile.toSchema(), WireBuilders.fromProtoFile(protoFile).toSchema());
	}

	/**
	 * A file that declares no syntax is proto2, and wire keeps that syntax null rather than resolving it. The mutable model has to carry the null through:
	 * substituting {@link Syntax#PROTO_2} would make every round trip add a {@code syntax = "proto2";} line to a file nothing had edited.
	 */
	@Test
	public void testFileWithoutSyntaxDeclarationDoesNotGainOne() {
		String source = """
				package fidelity;

				message Envelope {
				  required string id = 1;
				}
				""";

		ProtoFile protoFile = ProtoFile.Companion.get(ProtoParser.Companion.parse(Location.get("implicit_proto2.proto"), source));

		String schema = WireBuilders.fromProtoFile(protoFile).toSchema();

		assertFalse(schema.contains("syntax"), schema);
		assertEquals(protoFile.toSchema(), schema);
	}

	/**
	 * Stock wire escapes a top level string option but not one nested in an aggregate (message literal) option, while its parser unescapes both. Without the
	 * workaround in {@code WireBuilders} a round trip through the modify path drops one level of escaping and emits proto that protoc rejects.
	 */
	@Test
	public void testRoundTripKeepsEscapingInAggregateOptionValues() {
		String source = "syntax = \"proto3\";\n" + "package test;\n" + "\n" + "message M {\n"
				+ "  string a = 1 [(buf.validate.field).string = {pattern: \"^[a-z0-9\\\\-]+$\", min_len: 10}];\n"
				+ "  string b = 2 [(buf.validate.field).string.pattern = \"^[a-z0-9\\\\-]+$\"];\n" + "}\n";

		ProtoFile protoFile = ProtoFile.Companion.get(ProtoParser.Companion.parse(Location.get("options.proto"), source));

		String schema = WireBuilders.fromProtoFile(protoFile).toSchema();

		// The aggregate form keeps its escaping instead of degrading to the invalid "\-".
		assertTrue(schema.contains("pattern: \"^[a-z0-9\\\\-]+$\""), schema);
		// The plain form, which wire already escapes, must not be escaped a second time.
		assertTrue(schema.contains(".string.pattern = \"^[a-z0-9\\\\-]+$\""), schema);
	}

	/**
	 * Services, RPCs and extend declarations are carried through as wire parsed them rather than converted into the mutable model, so their aggregate options
	 * need the same escaping - including an extend nested in a message.
	 */
	@Test
	public void testRoundTripKeepsEscapingInPreservedServiceAndExtendOptions() {
		String source = """
				syntax = "proto2";
				package test;

				message M {
				  optional string id = 1;
				  extensions 100 to 200;
				  extend M {
				    optional string nested = 101 [(opt) = {pattern: "nested\\\\-y"}];
				  }
				}

				extend M {
				  optional string top = 100 [(opt) = {pattern: "top\\\\-y"}];
				}

				service S {
				  option (opt) = {pattern: "service\\\\-y"};
				  rpc Get (M) returns (M) {
				    option (opt) = {pattern: "rpc\\\\-y"};
				  }
				}
				""";

		ProtoFile protoFile = ProtoFile.Companion.get(ProtoParser.Companion.parse(Location.get("preserved.proto"), source));

		String schema = WireBuilders.fromProtoFile(protoFile).toSchema();

		for (String prefix : List.of("nested", "top", "service", "rpc")) {
			assertTrue(schema.contains("pattern: \"" + prefix + "\\\\-y\""), schema);
		}
	}

	/**
	 * The XSD-to-proto path already stores option strings in proto source form (see {@code ValidationRuleFactory}), so options that never went through wire's
	 * parser must be emitted verbatim rather than escaped a second time.
	 */
	@Test
	public void testGeneratedAggregateOptionValuesAreNotEscapedTwice() {
		String escapedPattern = "^[a-z0-9\\\\-]+$";
		MutableOptions fieldOptions = new MutableOptions(MutableOptions.FIELD_OPTIONS, new ArrayList<>());
		fieldOptions.add(new OptionElement("buf.validate.field", OptionElement.Kind.MAP, Map.of("string", Map.of("pattern", escapedPattern)), true));

		MutableProtoFile f = new MutableProtoFile(Syntax.PROTO_3, "test");
		Location location = new Location("base", "path", 1, 1);
		MutableMessageType message = new MutableMessageType(ProtoType.get("M"), location, "", "M",
				new MutableOptions(MutableOptions.MESSAGE_OPTIONS, new ArrayList<>()));
		message.addField(new MutableField(null, location, Field.Label.OPTIONAL, "a", "", 1, "string", fieldOptions, true));
		f.types().add(message);

		String schema = f.toSchema();

		assertTrue(schema.contains("pattern: \"" + escapedPattern + "\""), schema);
	}

	/**
	 * The same asymmetry reaches further in than a scalar entry: {@code formatOptionMapValue} recurses into lists and nested message literals and appends every
	 * string it finds verbatim, so escaping has to be restored at every depth, not just at the top of the aggregate.
	 */
	@Test
	public void testRoundTripKeepsEscapingDeepInsideAggregateOptions() {
		String source = "syntax = \"proto3\";\n" + "package test;\n" + "\n" + "message M {\n"
				+ "  string a = 1 [(buf.validate.field).string = {in: [\"x\\\\-y\", \"z\"], not_in: [{pattern: \"p\\\\-q\"}]}];\n" + "}\n";

		ProtoFile protoFile = ProtoFile.Companion.get(ProtoParser.Companion.parse(Location.get("listoptions.proto"), source));

		String schema = WireBuilders.fromProtoFile(protoFile).toSchema();

		assertTrue(schema.contains("\"x\\\\-y\""), schema);
		assertTrue(schema.contains("\"p\\\\-q\""), schema);
	}

	/** Verifies stock wire serializes extend declarations (used when modifying existing protos that contain them). */
	@Test
	public void testBuildExtension() {
		Location loc = new Location("", "", 0, 0);

		List<OptionElement> optionElements = new ArrayList<>();
		// The extendee is google.protobuf.MessageOptions, but options declared on the extension field itself are still field options.
		Options options = new Options(Options.FIELD_OPTIONS, optionElements);
		Field field = new Field(Collections.emptyList(), loc, null, "fieldname", "Base type this message actually is an extension of", 1101, null, "string",
				options, true, false, null);
		Extend extend = new Extend(loc, "Information elements extracted from the xsd structure", "google.protobuf.MessageOptions",
				Collections.singletonList(field));

		ProtoFile f = new ProtoFile(loc, new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), "default", new ArrayList<>(), new ArrayList<>(),
				Collections.singletonList(extend), new Options(Options.FILE_OPTIONS, new ArrayList<>()), Syntax.PROTO_3);

		String schema = f.toSchema();
		assertNotNull(schema);
	}
}
