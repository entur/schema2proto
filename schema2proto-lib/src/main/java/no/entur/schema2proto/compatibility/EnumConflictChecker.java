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

import static no.entur.schema2proto.compatibility.ConflictResolverHelper.createBiMap;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
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

			// Compute helper maps
			BiMap<String, Integer> xsdConstantsNameToId = createBiMap(newEnumConstantsInXsd);
			Map<Integer, String> xsdConstantsIdToName = xsdConstantsNameToId.inverse();

			BiMap<String, Integer> newConstantsInLockMapNameToId = createBiMap(surplusLockEnumConstants);
			Map<Integer, String> newConstantsInLockMapIdToName = newConstantsInLockMapNameToId.inverse();

			TreeSet<String> overlappingNames = new TreeSet<>(xsdConstantsNameToId.keySet());
			overlappingNames.retainAll(newConstantsInLockMapNameToId.keySet());

			TreeSet<Integer> overlappingIds = new TreeSet<>(xsdConstantsNameToId.values());
			overlappingIds.retainAll(newConstantsInLockMapNameToId.values());

			if (!(overlappingIds.isEmpty() && overlappingNames.isEmpty())) {
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Incompatible changes in proto {} {} , overlapping enum constant ids={}, overlapping enum constant names={}", file.name(),
							enumType.name(), overlappingIds, overlappingNames);
				}

				// If an existing constant.name (in both proto and protolock) has a new field number, updated constant.id to this number. If this number has
				// been used
				// for
				// another constant, assign this constant to a new id in a "safe" number range
				AtomicInteger nextAvailableConstantId = findNextAvailableFieldNum(enumType, xsdEnumConstants);

				if (!overlappingIds.isEmpty()) {
					// Check if the new field is using an already allocated id
					int overlappingId = overlappingIds.first();
					String originalFieldNameUsingThisId = newConstantsInLockMapIdToName.get(overlappingId);
					if (originalFieldNameUsingThisId != null) {
						// Find field that has take newFields original number
						String intrudingFieldName = xsdConstantsIdToName.get(overlappingId);
						Optional<EnumConstant> intrudingConstant = getConstant(enumType, intrudingFieldName);
						Optional<EnumConstant> existingConstant = getConstant(enumType, originalFieldNameUsingThisId);

						Integer idFromLockFile = newConstantsInLockMapNameToId.get(intrudingFieldName);
						updateEnumConstantId(nextAvailableConstantId, overlappingId, intrudingConstant, existingConstant, idFromLockFile);
					}

				} else if (!overlappingNames.isEmpty()) {

					String overlappingName = overlappingNames.first();

					// Check if the new field is using an already allocated name (changed id)
					Integer originalFieldIdForNewName = newConstantsInLockMapNameToId.get(overlappingName);
					if (originalFieldIdForNewName != null) {
						// Find field that has take newFields original number
						Integer intrudingFieldId = xsdConstantsNameToId.get(overlappingName);
						Optional<EnumConstant> intrudingConstant = getConstant(enumType, intrudingFieldId);
						Optional<EnumConstant> existingConstant = getConstant(enumType, originalFieldIdForNewName);

						Integer idFromLockFile = newConstantsInLockMapNameToId.get(overlappingName);
						updateEnumConstantId(nextAvailableConstantId, originalFieldIdForNewName, intrudingConstant, existingConstant, idFromLockFile);
					}
				}
				tryResolveEnumConflicts(file, enumType, protolockEnum);

			} else {
				// If neither overlapping constants names nor ids, no problem. Add reserved keyword for removed constants
				surplusLockEnumConstants.stream().forEach(newConstant -> {
					reserveEnumConstant(file, enumType, newConstant);
					LOGGER.debug("Removed constant in proto {}: {}, adding reserved section", file.name(), newConstant);
				});
			}

		}

		return failIfRemovedFieldsTriggered;
	}

	private void updateEnumConstantId(AtomicInteger nextAvailableFieldNum, int overlappingId, Optional<EnumConstant> intrudingField,
			Optional<EnumConstant> existingField, Integer idFromLockFile) {
		intrudingField.ifPresent(x -> {
			if (idFromLockFile != null) {
				x.updateTag(idFromLockFile);
			} else {
				x.updateTag(nextAvailableFieldNum.get());
			}
		});

		existingField.ifPresent(x -> x.updateTag(overlappingId));
	}

	private Optional<EnumConstant> getConstant(EnumType e, Integer intrudingConstantId) {
		return e.constants().stream().filter(z -> z.getTag() == intrudingConstantId).findFirst();
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

	@NotNull
	private AtomicInteger findNextAvailableFieldNum(EnumType e, SortedSet<ProtolockEnumConstant> xsdFields) {
		AtomicInteger nextAvailableFieldNum = new AtomicInteger(
				xsdFields.stream().max(Comparator.comparing(ProtolockEnumConstant::getId)).orElse(new ProtolockEnumConstant(0, null)).getId() + 1);

		// Check that it is not reserved
		while (e.reserveds().stream().anyMatch(s -> s.matchesTag(nextAvailableFieldNum.get()))) {
			nextAvailableFieldNum.incrementAndGet();
		}
		return nextAvailableFieldNum;
	}

	private static Optional<EnumConstant> getConstant(EnumType e, String intrudingConstantName) {
		return e.constants().stream().filter(z -> z.getName().equals(intrudingConstantName)).findFirst();
	}

}
