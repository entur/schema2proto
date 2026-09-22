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

import com.squareup.wire.schema.internal.parser.ReservedElement;

import kotlin.ranges.IntRange;

/**
 * Tag and name matching for reservations, mirroring {@code com.squareup.wire.schema.Reserved.matchesTag} and {@code matchesName}. The element layer holds the
 * reserved values as a plain list of {@code Integer}, {@code IntRange} and {@code String}, with no matching of its own.
 */
public final class Reservations {

	private Reservations() {
	}

	static boolean matchesTag(List<ReservedElement> reserveds, int tag) {
		return reserveds.stream().anyMatch(reserved -> matchesTag(reserved, tag));
	}

	static boolean matchesName(List<ReservedElement> reserveds, String name) {
		return reserveds.stream().anyMatch(reserved -> reserved.getValues().stream().anyMatch(name::equals));
	}

	/**
	 * Returns the reservation with {@code name} and {@code tag} released, or {@code null} when nothing is left to reserve. A reserved range covering the tag is
	 * split around it, so releasing 150 from {@code reserved 100 to 199} leaves {@code reserved 100 to 149, 151 to 199} rather than a range that still covers
	 * the tag being handed back.
	 *
	 * @param tag the tag to release, or -1 to release the name only.
	 */
	public static ReservedElement released(ReservedElement reserved, String name, int tag) {
		List<Object> remaining = new ArrayList<>(reserved.getValues().size());
		for (Object value : reserved.getValues()) {
			if (value instanceof String reservedName && reservedName.equals(name)) {
				continue;
			}
			if (value instanceof Integer reservedTag && reservedTag == tag) {
				continue;
			}
			if (value instanceof IntRange range && range.getFirst() <= tag && tag <= range.getLast()) {
				addTags(remaining, range.getFirst(), tag - 1);
				addTags(remaining, tag + 1, range.getLast());
				continue;
			}
			remaining.add(value);
		}
		return remaining.isEmpty() ? null : new ReservedElement(reserved.getLocation(), reserved.getDocumentation(), remaining);
	}

	/** Appends the tags from {@code first} to {@code last} as wire represents them: a lone tag as an {@code Integer}, several as an {@code IntRange}. */
	private static void addTags(List<Object> target, int first, int last) {
		if (first > last) {
			return;
		}
		target.add(first == last ? Integer.valueOf(first) : new IntRange(first, last));
	}

	private static boolean matchesTag(ReservedElement reserved, int tag) {
		for (Object value : reserved.getValues()) {
			if (value instanceof Integer reservedTag && reservedTag == tag) {
				return true;
			}
			if (value instanceof IntRange range && range.getFirst() <= tag && tag <= range.getLast()) {
				return true;
			}
		}
		return false;
	}
}
