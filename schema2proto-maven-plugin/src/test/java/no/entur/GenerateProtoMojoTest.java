package no.entur;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Rule;
import org.junit.Test;

public class GenerateProtoMojoTest {
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
	public void testGenerateSimpleProto() throws Exception {

		File pom = new File("src/test/resources/generate");
		assertNotNull(pom);
		assertTrue(pom.exists());

		GenerateProtoMojo myMojo = (GenerateProtoMojo) rule.lookupConfiguredMojo(pom, "generate");
		assertNotNull(myMojo);

		// Set properties on goal
		rule.setVariableValueToObject(myMojo, "configFile", new File("src/test/resources/generate/simple.yml"));
		rule.setVariableValueToObject(myMojo, "xsdFile", new File("src/test/resources/generate/simple.xsd"));

		// Execute
		myMojo.execute();

		// Assert proto file is generated
		File protoResultFile = new File("target/generated-proto/simple.proto");
		assertNotNull(protoResultFile);
		assertTrue(protoResultFile.exists());
	}

}
