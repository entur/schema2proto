package no.entur.schema2proto;

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
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class ReduceProtoTest {

	@Test
	public void testRemoveIndependentMessageType() throws IOException {

		File expected = new File("src/test/resources/reduce/simple/expected").getCanonicalFile();
		File source = new File("src/test/resources/reduce/simple/source").getCanonicalFile();

		List<String> excludes = new ArrayList<>();
		excludes.add("A");
		List<String> includes = new ArrayList<>();
		File actual = TestHelper.reduce(source, includes, excludes);

		TestHelper.compareExpectedAndGenerated(expected, "missing_a.proto", actual, "simple.proto");

	}

	@Test
	public void testWhitelistMessageType() throws IOException {

		File expected = new File("src/test/resources/reduce/simple/expected").getCanonicalFile();
		File source = new File("src/test/resources/reduce/simple/source").getCanonicalFile();

		List<String> excludes = new ArrayList<>();

		List<String> includes = new ArrayList<>();
		includes.add("A");
		File actual = TestHelper.reduce(source, includes, excludes);

		TestHelper.compareExpectedAndGenerated(expected, "only_a.proto", actual, "simple.proto");

	}

	@Test
	public void testRemoveFieldAndType() throws IOException {

		File expected = new File("src/test/resources/reduce/simple/expected").getCanonicalFile();
		File source = new File("src/test/resources/reduce/simple/source").getCanonicalFile();

		List<String> excludes = new ArrayList<>();
		excludes.add("LangType");
		List<String> includes = new ArrayList<>();

		File actual = TestHelper.reduce(source, includes, excludes);

		TestHelper.compareExpectedAndGenerated(expected, "no_langtype.proto", actual, "simple.proto");

	}

	@Test
	public void testWhitelistMessageTypeAndDependency() throws IOException {

		File expected = new File("src/test/resources/reduce/simple/expected").getCanonicalFile();
		File source = new File("src/test/resources/reduce/simple/source").getCanonicalFile();

		List<String> excludes = new ArrayList<>();

		List<String> includes = new ArrayList<>();
		includes.add("B");
		File actual = TestHelper.reduce(source, includes, excludes);

		TestHelper.compareExpectedAndGenerated(expected, "missing_a.proto", actual, "simple.proto");

	}

}
