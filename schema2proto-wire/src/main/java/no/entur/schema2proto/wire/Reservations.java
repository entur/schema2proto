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

import java.util.ArrayList;
import java.util.List;

import com.squareup.wire.schema.Reserved;

import kotlin.ranges.IntRange;

/**
 * Releasing part of a reservation, which wire's {@link Reserved} has no support for. Matching is left to {@link Reserved#matchesTag} and
 * {@link Reserved#matchesName}.
 */
public final class Reservations {

	private Reservations() {
	}

	/**
	 * Returns the reservation with {@code name} and {@code tag} released, or {@code null} when nothing is left to reserve. A reserved range covering the tag is
	 * split around it, so releasing 150 from {@code reserved 100 to 199} leaves {@code reserved 100 to 149, 151 to 199} rather than a range that still covers
	 * the tag being handed back.
	 *
	 * @param tag the tag to release, or {@code null} to release the name only. Any int is a valid tag here, since enum values may be negative.
	 */
	public static Reserved released(Reserved reserved, String name, Integer tag) {
		List<Object> remaining = new ArrayList<>(reserved.getValues().size());
		for (Object value : reserved.getValues()) {
			if (value instanceof String reservedName && reservedName.equals(name)) {
				continue;
			}
			if (tag != null) {
				int released = tag;
				if (value instanceof Integer reservedTag && reservedTag == released) {
					continue;
				}
				if (value instanceof IntRange range && range.getFirst() <= released && released <= range.getLast()) {
					// Guard the neighbours so a tag at Integer.MIN_VALUE or MAX_VALUE does not wrap around into a range spanning every int
					if (released > range.getFirst()) {
						addTags(remaining, range.getFirst(), released - 1);
					}
					if (released < range.getLast()) {
						addTags(remaining, released + 1, range.getLast());
					}
					continue;
				}
			}
			remaining.add(value);
		}
		return remaining.isEmpty() ? null : new Reserved(reserved.getLocation(), reserved.getDocumentation(), remaining);
	}

	/** Appends the tags from {@code first} to {@code last} as wire represents them: a lone tag as an {@code Integer}, several as an {@code IntRange}. */
	private static void addTags(List<Object> target, int first, int last) {
		if (first > last) {
			return;
		}
		target.add(first == last ? Integer.valueOf(first) : new IntRange(first, last));
	}
}
