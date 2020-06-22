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
package no.entur;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import no.entur.schema2proto.InvalidConfigurationException;
import no.entur.schema2proto.modifyproto.InvalidProtobufException;
import no.entur.schema2proto.modifyproto.ModifyProto;

@Mojo(name = "modify", defaultPhase = LifecyclePhase.PROCESS_RESOURCES)
public class ModifyProtoMojo extends AbstractMojo {

	/**
	 * Configuration file for schema2proto
	 */
	@Parameter(required = true)
	private File configFile;

	@Parameter(readonly = true, defaultValue = "${project}")
	private MavenProject project;

	public void execute() throws MojoExecutionException {

		if (configFile == null || !configFile.exists()) {
			throw new MojoExecutionException("Config file not found");
		}

		getLog().info(String.format("Modifying proto files from using config file %s. Output is defined in config file", configFile));

		try {
			new ModifyProto().modifyProto(configFile, project.getBasedir());
		} catch (IOException e) {
			throw new MojoExecutionException("Error modifying proto files", e);
		} catch (InvalidConfigurationException e) {
			throw new MojoExecutionException("Invalid modify configuration file", e);
		} catch (InvalidProtobufException e) {
			throw new MojoExecutionException("Could not fully modify proto files", e);
		}

	}
}
