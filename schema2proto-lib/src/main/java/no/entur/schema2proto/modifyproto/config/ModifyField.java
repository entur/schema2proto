/*-
 * #%L
 * schema2proto-lib
 * %%
 * Copyright (C) 2019 - 2020 Entur
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

public class ModifyField {
	/**
	 * Name of the existing message type.
	 */
	public String targetMessageType;

	/**
	 * Name of the field on the existing message type to modify.
	 */
	public String field;

	/**
	 * Regex pattern to match in the existing documentation. If not provided, the existing documentation is completely replaced by
	 * {@link ModifyField#documentation}.
	 */
	public String documentationPattern;

	/**
	 * Replacement string for the documentation of the existing field. Regex capturing groups from the {@link ModifyField#documentationPattern} are supported.
	 */
	public String documentation;
}
