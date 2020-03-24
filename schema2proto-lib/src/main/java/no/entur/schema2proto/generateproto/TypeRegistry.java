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
package no.entur.schema2proto.generateproto;

import java.util.HashSet;
import java.util.Set;

public class TypeRegistry {
	public static Set<String> getBasicTypes() {

		Set<String> basicTypes = new HashSet<>();
		basicTypes.add("string");
		basicTypes.add("boolean");
		basicTypes.add("float");
		basicTypes.add("double");
		basicTypes.add("decimal");
		basicTypes.add("duration");
		basicTypes.add("dateTime");
		basicTypes.add("time");
		basicTypes.add("date");

		basicTypes.add("gYearMonth");
		basicTypes.add("gYear");
		basicTypes.add("gMonthDay");
		basicTypes.add("gDay");
		basicTypes.add("gMonth");

		basicTypes.add("hexBinary");
		basicTypes.add("base64Binary");
		basicTypes.add("anyURI");
		basicTypes.add("QName");
		basicTypes.add("NOTATION");

		basicTypes.add("normalizedString");
		basicTypes.add("token");
		basicTypes.add("language");

		basicTypes.add("IDREFS");
		basicTypes.add("ENTITIES");
		basicTypes.add("NMTOKEN");
		basicTypes.add("NMTOKENS");
		basicTypes.add("Name");
		basicTypes.add("NCName");
		basicTypes.add("ID");
		basicTypes.add("IDREF");
		basicTypes.add("ENTITY");

		basicTypes.add("integer");
		basicTypes.add("nonPositiveInteger");
		basicTypes.add("negativeInteger");
		basicTypes.add("long");
		basicTypes.add("int");
		basicTypes.add("short");
		basicTypes.add("byte");

		basicTypes.add("nonNegativeInteger");
		basicTypes.add("unsignedLong");
		basicTypes.add("unsignedInt");
		basicTypes.add("unsignedShort");
		basicTypes.add("unsignedByte");
		basicTypes.add("positiveInteger");

		basicTypes.add("anySimpleType");
		basicTypes.add("anyType");

		return basicTypes;

	}

}
