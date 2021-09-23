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
package no.entur.schema2proto.modifyproto.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ModifyProtoConfiguration {
	public File inputDirectory;
	public File outputDirectory;
	public File basedir;
	public List<String> includes = new ArrayList<>();
	public List<String> excludes = new ArrayList<>();
	public List<NewField> newFields = new ArrayList<>();
	public List<MergeFrom> mergeFrom = new ArrayList<>();
	public List<String> customImportLocations = new ArrayList<>();
	public List<NewEnumConstant> newEnumConstants = new ArrayList<>();
	public List<FieldOption> fieldOptions = new ArrayList<>();
	public boolean includeBaseTypes = false;
	public File protoLockFile;
	public boolean failIfRemovedFields = true;
	public boolean includeGoPackageOptions = false;
	public String goPackageSourcePrefix = null;

}
