package no.entur.schema2proto.wire;

/*-
 * #%L
 * schema2proto Maven Plugin
 * %%
 * Copyright (C) 2019 - 2026 Entur
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import com.squareup.wire.schema.Field.Label;
import com.squareup.wire.schema.Location;

public class MutableFieldTest {

	/**
	 * The short constructor is the one the XSD-to-proto path uses, and its trailing boolean says whether the field came from an XSD element (as opposed to an
	 * attribute). Fields built this way are never proto extensions.
	 */
	@Test
	public void testShortConstructorTrailingBooleanIsFromElement() {
		MutableField field = newField(true);

		assertTrue(field.isFromElement());
		assertFalse(field.isExtension());

		assertFalse(newField(false).isFromElement());
	}

	private MutableField newField(boolean fromElement) {
		return new MutableField("pkg", Location.get("file.proto"), Label.OPTIONAL, "name", "doc", 1, "string",
				new MutableOptions(MutableOptions.FIELD_OPTIONS, new ArrayList<>()), fromElement);
	}
}
