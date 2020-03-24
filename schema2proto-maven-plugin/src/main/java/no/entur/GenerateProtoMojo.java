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

import java.io.File;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import no.entur.schema2proto.generateproto.Schema2Proto;

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

		try {
			if (configFile == null || !configFile.exists()) {
				throw new MojoExecutionException("Config file not found");
			}

			if (xsdFile == null || !xsdFile.exists()) {
				throw new MojoExecutionException("XSD file not found");
			}

			getLog().info(String.format("Generating proto files from %s using config file %s. Output is defined in config file", xsdFile, configFile));

			Schema2Proto.main(new String[] { "--configFile=" + configFile, "" + xsdFile });
		} catch (MojoExecutionException e) {
			throw new MojoExecutionException("Error generating proto files", e);
		}

	}
}
