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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.squareup.wire.Syntax;
import com.squareup.wire.schema.Extend;
import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.Options;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.ProtoType;
import com.squareup.wire.schema.internal.parser.OptionElement;

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

	/** Verifies stock wire serializes extend declarations (used when modifying existing protos that contain them). */
	@Test
	public void testBuildExtension() {
		Location loc = new Location("", "", 0, 0);

		List<OptionElement> optionElements = new ArrayList<>();
		Options options = new Options(ProtoType.get("google.protobuf.MessageOptions"), optionElements);
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
