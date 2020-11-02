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
package no.entur.schema2proto.compatibility.protolock;

import java.util.Objects;

import com.google.gson.annotations.SerializedName;

public class ProtolockEnumConstant extends AbstractNameIDPair {

	private String name;
	@SerializedName("integer")
	private int id;

	public ProtolockEnumConstant(int integer, String name) {
		this.id = integer;
		this.name = name;

	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ProtolockEnumConstant that = (ProtolockEnumConstant) o;
		return getId() == that.getId() && Objects.equals(getName(), that.getName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getName(), getId());
	}

	@Override
	public String toString() {
		return "ProtolockEnumField{" + "id=" + getId() + ", name='" + getName() + '\'' + '}';
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getId() {
		return id;
	}
}
