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
package no.entur.schema2proto.modifyproto;

import java.util.List;

import no.entur.schema2proto.modifyproto.config.FieldOption;
import no.entur.schema2proto.modifyproto.config.MergeFrom;
import no.entur.schema2proto.modifyproto.config.ModifyField;
import no.entur.schema2proto.modifyproto.config.NewEnumConstant;
import no.entur.schema2proto.modifyproto.config.NewField;

public class ModifyProtoConfigFile {
	public String inputDirectory;
	public String outputDirectory;
	public List<String> includes;
	public List<String> excludes;
	public List<NewField> newFields;
	public List<ModifyField> modifyFields;
	public List<MergeFrom> mergeFrom;
	public List<String> customImportLocations;
	public List<NewEnumConstant> newEnumConstants;
	public boolean includeBaseTypes;
	public List<FieldOption> fieldOptions;
	public String protoLockFile;
	public boolean failIfRemovedFields = true;
	/**
	 * Fail if a field had to be given a new number because the number it was declared with was already taken or reserved in proto.lock. Such a renumbering
	 * silently breaks wire compatibility with the input, so it is worth failing on. Defaults to false to preserve existing behaviour.
	 */
	public boolean failIfFieldsRenumbered = false;
	public boolean includeGoPackageOptions = false;
	public String goPackageSourcePrefix = null;
}
