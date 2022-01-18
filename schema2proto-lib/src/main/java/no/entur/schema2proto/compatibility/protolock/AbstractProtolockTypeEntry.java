package no.entur.schema2proto.compatibility.protolock;

/*-
 * #%L
 * schema2proto-lib
 * %%
 * Copyright (C) 2019 - 2022 Entur
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

import com.google.gson.annotations.SerializedName;

public abstract class AbstractProtolockTypeEntry {
	@SerializedName(("reserved_names"))
	String[] reservedNames;
	@SerializedName("reserved_ids")
	Integer[] reservedIds;

	public Integer[] getReservedIds() {
		return reservedIds;
	}

	public String[] getReservedNames() {
		return reservedNames;
	}

}
