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
package no.entur.schema2proto.wire;

import java.util.List;

import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.ProtoType;
import com.squareup.wire.schema.internal.parser.TypeElement;

/** Mutable builder analogue of the stock wire {@link com.squareup.wire.schema.Type} hierarchy. */
public abstract class MutableType {

	public abstract ProtoType type();

	public abstract Location location();

	public abstract String documentation();

	public abstract void updateDocumentation(String documentation);

	public abstract MutableOptions options();

	public abstract List<MutableType> nestedTypes();

	/**
	 * Renders this type back to wire's element AST, the layer schema2proto both reads from and writes to. Implementations that were built from an existing
	 * element copy it rather than construct a fresh one, so that whatever this model does not represent survives the round trip.
	 */
	public abstract TypeElement toElement();
}
