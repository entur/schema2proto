package no.entur.schema2proto.compatibility.protolock;

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

import org.jetbrains.annotations.NotNull;

public abstract class AbstractNameIDPair implements Comparable<AbstractNameIDPair> {

	public abstract String getName();

	public abstract int getId();

	@Override
	public int compareTo(@NotNull AbstractNameIDPair abstractNameIDPair) {
		if (getId() == abstractNameIDPair.getId()) {
			return getName().compareTo(abstractNameIDPair.getName());
		} else {
			return Integer.compare(getId(), abstractNameIDPair.getId());
		}
	}
}
