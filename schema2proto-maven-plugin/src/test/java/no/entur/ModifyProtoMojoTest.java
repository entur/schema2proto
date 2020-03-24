package no.entur;

/*-
 * #%L
 * schema2proto Maven Plugin
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;
import org.junit.Rule;
import org.junit.Test;

public class ModifyProtoMojoTest {
	@Rule
	public MojoRule rule = new MojoRule() {
		@Override
		protected void before() throws Throwable {

		}

		@Override
		protected void after() {
		}
	};

	@Test
	public void testModifyProto() throws Exception {

		File pom = new File("src/test/resources/modify");
		assertNotNull(pom);
		assertTrue(pom.exists());

		ModifyProtoMojo myMojo = (ModifyProtoMojo) rule.lookupConfiguredMojo(pom, "modify");
		assertNotNull(myMojo);

		// Set properties on goal
		rule.setVariableValueToObject(myMojo, "configFile", new File("src/test/resources/modify/modify.yml"));
		rule.setVariableValueToObject(myMojo, "project", new MavenProjectStub());

		// Execute
		myMojo.execute();

		// Assert proto file is generated
		File protoResultFile = new File("target/generated-proto/modify.proto");
		assertNotNull(protoResultFile);
		assertTrue(protoResultFile.exists());
	}

}
