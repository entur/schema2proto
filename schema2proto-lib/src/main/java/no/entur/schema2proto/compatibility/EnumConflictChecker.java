package no.entur.schema2proto.compatibility;

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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squareup.wire.schema.EnumConstant;
import com.squareup.wire.schema.EnumType;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.ProtoFile;

import no.entur.schema2proto.compatibility.protolock.ProtolockEnum;
import no.entur.schema2proto.compatibility.protolock.ProtolockEnumConstant;

public class EnumConflictChecker {

	private final static Logger LOGGER = LoggerFactory.getLogger(EnumConflictChecker.class);
	private boolean failIfRemovedFieldsTriggered;

	public boolean tryResolveEnumConflicts(ProtoFile file, EnumType enumType, ProtolockEnum protolockEnum) {
		SortedSet<ProtolockEnumConstant> lockEnumConstants = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(protolockEnum.getEnumFields()))); // from
		// proto.lock
		SortedSet<ProtolockEnumConstant> xsdEnumConstants = Collections.unmodifiableSortedSet(
				new TreeSet<>(enumType.constants().stream().map(f -> new ProtolockEnumConstant(f.getTag(), f.getName())).collect(Collectors.toSet()))); // from
		// parsed
		// Find fields that are new
		Set<ProtolockEnumConstant> newEnumConstantsInXsd = new TreeSet<>(xsdEnumConstants); // from parsed / converted xsd
		newEnumConstantsInXsd.removeAll(lockEnumConstants);

		Set<ProtolockEnumConstant> surplusLockEnumConstants = new TreeSet<>(lockEnumConstants); // from proto.lock
		surplusLockEnumConstants.removeAll(xsdEnumConstants);

		if (newEnumConstantsInXsd.isEmpty() && surplusLockEnumConstants.isEmpty()) {
			// No mismatch, only minor details
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("No added or removed constants in in proto {} {}", file.name(), enumType.name());
			}
		} else if (newEnumConstantsInXsd.isEmpty() && !surplusLockEnumConstants.isEmpty()) {
			// Find constants that are removed (make reserved)
			surplusLockEnumConstants.stream().forEach(newField -> reserveEnumConstant(file, enumType, newField));

		} else if (!newEnumConstantsInXsd.isEmpty() && surplusLockEnumConstants.isEmpty()) {
			// Only new enum constants from xsd
			newEnumConstantsInXsd.stream().forEach(newField -> LOGGER.debug("Added field in proto {} {} : {}", file.name(), enumType.name(), newField));
		} else {
			// Phase 1: Restore lock-file constants to their original IDs
			for (ProtolockEnumConstant lockConstant : lockEnumConstants) {
				getConstant(enumType, lockConstant.getName()).ifPresent(c -> c.updateTag(lockConstant.getId()));
			}

			// Phase 2: Assign new constants (not in lock file) gap-free after max lock ID
			Set<String> lockNames = lockEnumConstants.stream().map(ProtolockEnumConstant::getName).collect(Collectors.toSet());

			int maxLockId = lockEnumConstants.stream()
					.max(Comparator.comparing(ProtolockEnumConstant::getId))
					.orElse(new ProtolockEnumConstant(0, null))
					.getId();

			AtomicInteger nextId = new AtomicInteger(maxLockId + 1);

			for (EnumConstant constant : enumType.constants()) {
				if (!lockNames.contains(constant.getName())) {
					while (enumType.reserveds().stream().anyMatch(s -> s.matchesTag(nextId.get()))) {
						nextId.incrementAndGet();
					}
					constant.updateTag(nextId.getAndIncrement());
				}
			}

			// Handle removed constants
			surplusLockEnumConstants.stream()
					.filter(lc -> getConstant(enumType, lc.getName()).isEmpty())
					.forEach(removed -> reserveEnumConstant(file, enumType, removed));
		}

		return failIfRemovedFieldsTriggered;
	}

	private void reserveEnumConstant(ProtoFile file, EnumType e, ProtolockEnumConstant newEnumConstant) {

		String reservationDoc = "Reservation added by schema2proto";
		Location loc = new Location("", "", 0, 0);

		// 2 reservations since field name and id cannot be on the same reservation list
		e.addReserved(reservationDoc, loc, newEnumConstant.getName());
		e.addReserved(reservationDoc, loc, newEnumConstant.getId());

		LOGGER.warn(
				"Possible backwards incompatibility detected, must be checked manually! Removed enum constant in proto {}, message {}, field {}, blocking enum name and id for future use by adding 'reserved' statement",
				file.name(), e.name(), newEnumConstant);
		failIfRemovedFieldsTriggered = true;
	}

	private static Optional<EnumConstant> getConstant(EnumType e, String intrudingConstantName) {
		return e.constants().stream().filter(z -> z.getName().equals(intrudingConstantName)).findFirst();
	}

}
