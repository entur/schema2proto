package no.entur;

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import no.entur.schema2proto.Schema2Proto;

/**
 * Generate proto file from set of xsd files
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class GenerateProtoMojo extends AbstractMojo {

	/**
	 * Configuration file for schema2proto
	 */
	@Parameter(required = true)
	private File configFile;

	/**
	 * XSD file to convert
	 */
	@Parameter(required = true)
	private File xsdFile;

	public void execute() throws MojoExecutionException {

		if (configFile == null || !configFile.exists()) {
			throw new MojoExecutionException("Config file not found");
		}

		if (xsdFile == null || !xsdFile.exists()) {
			throw new MojoExecutionException("XSD file not found");
		}

		getLog().info(String.format("Generating proto files from %s using config file %s. Output is defined in config file", xsdFile, configFile));

		Schema2Proto.main(new String[] { "--configFile=" + configFile, "" + xsdFile });

	}
}
