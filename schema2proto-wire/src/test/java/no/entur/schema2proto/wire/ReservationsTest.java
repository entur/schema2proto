package no.entur.schema2proto.wire;

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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.Reserved;

import kotlin.ranges.IntRange;

public class ReservationsTest {

	@Test
	public void testReleaseRangeStartAtIntMin_thenKeepOnlyTheRest() {
		Reserved released = Reservations.released(reserved(new IntRange(Integer.MIN_VALUE, Integer.MIN_VALUE + 2)), "X", Integer.MIN_VALUE);

		assertEquals(List.of(new IntRange(Integer.MIN_VALUE + 1, Integer.MIN_VALUE + 2)), released.getValues());
	}

	@Test
	public void testReleaseRangeEndAtIntMax_thenKeepOnlyTheRest() {
		Reserved released = Reservations.released(reserved(new IntRange(Integer.MAX_VALUE - 2, Integer.MAX_VALUE)), "X", Integer.MAX_VALUE);

		assertEquals(List.of(new IntRange(Integer.MAX_VALUE - 2, Integer.MAX_VALUE - 1)), released.getValues());
	}

	@Test
	public void testReleaseNegativeTag() {
		assertNull(Reservations.released(reserved(-1), "X", -1));
		assertEquals(List.of(-3, -1), Reservations.released(reserved(new IntRange(-3, -1)), "X", -2).getValues());
	}

	@Test
	public void testReleaseNameOnly_thenKeepEveryTag() {
		Reserved released = Reservations.released(reserved(-1, new IntRange(1, 3), "X"), "X", null);

		assertEquals(List.of(-1, new IntRange(1, 3)), released.getValues());
	}

	private static Reserved reserved(Object... values) {
		return new Reserved(Location.get("file.proto"), "", List.of(values));
	}
}
