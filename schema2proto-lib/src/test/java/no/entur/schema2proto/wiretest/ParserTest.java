package no.entur.schema2proto.wiretest;

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

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.squareup.wire.schema.IdentifierSet;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.Schema;
import com.squareup.wire.schema.SchemaLoader;

public class ParserTest {

	@Test
	public void testParseWithFieldRules() throws IOException {
		SchemaLoader schemaLoader = new SchemaLoader();
		schemaLoader.addSource(new File("src/test/resources/wiretest/source"));
		schemaLoader.addSource(new File("src/test/resources/wiretest/include"));
		schemaLoader.addProto("packagename/fieldrulesloading.proto");
		Schema schema = schemaLoader.load();

		IdentifierSet.Builder b = new IdentifierSet.Builder();
		b.exclude("packagename.PruneMessage");
		Schema prunedSchema = schema.prune(b.build());

		ProtoFile protoFile = prunedSchema.protoFile("packagename/fieldrulesloading.proto");
		String prunedFile = protoFile.toSchema();

		Assertions.assertEquals("// src/test/resources/wiretest/source/packagename/fieldrulesloading.proto\n" + "syntax = \"proto3\";\n"
				+ "package packagename;\n" + "\n" + "import \"validate/validate.proto\";\n" + "\n" + "message PriceUnit {\n"
				+ "  SubMessage with_options_map_style = 1 [\n" + "    (validate.rules).message = {\n" + "      required: \"true\"\n" + "    }\n" + "  ];\n"
				+ "  SubMessage with_options_nested_style = 2 [\n" + "    (validate.rules).message.required = true\n" + "  ];\n" + "}\n"
				+ "message SubMessage {\n" + "  string x = 1;\n" + "}\n", prunedFile);

	}
}
