package no.entur.schema2proto;

import static no.entur.schema2proto.TestHelper.compareExpectedAndGenerated;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class MultipleNamespaceTest {

	@BeforeAll
	public static void generateProtobufForTests() {
		try {
			new File("target/generated-proto").mkdirs();
			Schema2Proto.main(new String[] { "--splitBySchema=true", "--directory=target/generated-proto/", "--package=schemas.com.domain.common",
					"src/test/resources/xsd/ns-person.xsd" });
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.toString());
		}
	}

	@Test
	public void shouldCreateANamespacedProtobufPersonFile() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/schemas_com_domain_person.proto",
				"target/generated-proto/schemas_com_domain_person.proto");
	}

	@Test
	public void shouldCreateANamespacedProtobufCommonFile() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/schemas_com_domain_common.proto",
				"target/generated-proto/schemas_com_domain_common.proto");
	}

	@Test
	public void shouldCreateANamespacedProtobufAddressFile() throws IOException {
		compareExpectedAndGenerated("src/test/resources/expectedproto/schemas_com_domain_address.proto",
				"target/generated-proto/schemas_com_domain_address.proto");
	}
}
