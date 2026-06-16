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
package no.entur.schema2proto.generateproto.wire;

import com.squareup.wire.schema.EnumConstant;
import com.squareup.wire.schema.Location;

/** Mutable builder analogue of {@link com.squareup.wire.schema.EnumConstant}. */
public class MutableEnumConstant {

	private final Location location;
	private String name;
	private int tag;
	private String documentation;
	private final MutableOptions options;

	public MutableEnumConstant(Location location, String name, int tag, String documentation, MutableOptions options) {
		this.location = location;
		this.name = name;
		this.tag = tag;
		this.documentation = documentation;
		this.options = options;
	}

	public Location getLocation() {
		return location;
	}

	public String getName() {
		return name;
	}

	public void updateName(String name) {
		this.name = name;
	}

	public int getTag() {
		return tag;
	}

	public void updateTag(int newTag) {
		this.tag = newTag;
	}

	public String getDocumentation() {
		return documentation;
	}

	public MutableOptions options() {
		return options;
	}

	public EnumConstant toWire() {
		return new EnumConstant(location, name, tag, documentation == null ? "" : documentation, options.toWire());
	}
}
