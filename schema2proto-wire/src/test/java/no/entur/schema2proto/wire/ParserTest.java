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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.PruningRules;
import com.squareup.wire.schema.Schema;

public class ParserTest {

	@Test
	public void testParseWithFieldRules() throws IOException {
		Schema schema = WireSchemaLoader.load(Arrays.asList(new File("src/test/resources/wiretest/source").toPath(), new File("target/proto_deps").toPath()),
				Collections.singletonList("packagename/fieldrulesloading.proto"));

		PruningRules rules = new PruningRules.Builder().prune("packagename.PruneMessage").build();
		Schema prunedSchema = schema.prune(rules);

		ProtoFile protoFile = prunedSchema.protoFile("packagename/fieldrulesloading.proto");
		String prunedFile = protoFile.toSchema();

		Assertions.assertEquals("// Proto schema formatted by Wire, do not edit.\n" + "// Source: packagename/fieldrulesloading.proto\n" + "\n"
				+ "syntax = \"proto3\";\n" + "\n" + "package packagename;\n" + "\n" + "import \"buf/validate/validate.proto\";\n" + "\n"
				+ "message PriceUnit {\n" + "  SubMessage with_options_nested_style = 1 [(buf.validate.field).required = true];\n" + "}\n" + "\n"
				+ "message SubMessage {\n" + "  string x = 1;\n" + "}\n", prunedFile);

	}
}
