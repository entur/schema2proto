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

package no.entur.schema2proto.compatibility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.squareup.wire.schema.EnumType;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.ProtoFile;

import no.entur.schema2proto.compatibility.protolock.ProtolockDefinition;
import no.entur.schema2proto.compatibility.protolock.ProtolockDefinitions;
import no.entur.schema2proto.compatibility.protolock.ProtolockEnum;
import no.entur.schema2proto.compatibility.protolock.ProtolockFile;
import no.entur.schema2proto.compatibility.protolock.ProtolockMessage;

public class ProtolockBackwardsCompatibilityChecker {

	private ProtolockDefinitions definitions = null;

	private EnumConflictChecker enumConflictChecker = new EnumConflictChecker();

	private FieldConflictChecker fieldConflictChecker = new FieldConflictChecker();

	private final String reservationDoc = "Reservation added by schema2proto";
	private final Location reservationLocation = new Location("", "", 0, 0);

	private static final Logger LOGGER = LoggerFactory.getLogger(ProtolockBackwardsCompatibilityChecker.class);

	public void init(File protoLockFile) throws FileNotFoundException {
		Gson gson = new Gson();
		definitions = gson.fromJson(new FileReader(protoLockFile), ProtolockDefinitions.class);
	}

	public ProtolockDefinitions getDefinitions() {
		return definitions;
	}

	private void copyReservations(ProtolockMessage protolockMessage, MessageType protoMessage) {
		if (protolockMessage.getReservedIds() != null && protolockMessage.getReservedIds().length > 0) {
			Arrays.stream(protolockMessage.getReservedIds()).forEach(reservedId -> protoMessage.addReserved(reservationDoc, reservationLocation, reservedId));
		}
		if (protolockMessage.getReservedNames() != null && protolockMessage.getReservedNames().length > 0) {
			Arrays.stream(protolockMessage.getReservedNames())
					.forEach(reservedName -> protoMessage.addReserved(reservationDoc, reservationLocation, reservedName));
		}

		protoMessage.nestedTypes().stream().filter(z -> z instanceof MessageType).map(k -> (MessageType) k).forEach(nestedType -> {
			if (protolockMessage.getMessages() != null) {
				Arrays.stream(protolockMessage.getMessages()).forEach(nestedProtolockMessage -> {
					if (nestedProtolockMessage.getName().equals(nestedType.getName())) {
						copyReservations(nestedProtolockMessage, nestedType);
					}
				});
			}
		});

	}

	private void copyReservations(ProtolockEnum protolockEnum, EnumType protoEnum) {
		LOGGER.debug("Copying reservations for message {} and enum {}", protolockEnum, protoEnum);
		if (protolockEnum.getReservedIds() != null && protolockEnum.getReservedIds().length > 0) {
			Arrays.stream(protolockEnum.getReservedIds()).forEach(reservedId -> protoEnum.addReserved(reservationDoc, reservationLocation, reservedId));
		}
		if (protolockEnum.getReservedNames() != null && protolockEnum.getReservedNames().length > 0) {
			Arrays.stream(protolockEnum.getReservedNames()).forEach(reservedName -> protoEnum.addReserved(reservationDoc, reservationLocation, reservedName));
		}
	}

	public boolean resolveBackwardIncompatibilities(ProtoFile protoFile) {
		LOGGER.debug("Trying to resolve backward incompabilities in file {}", protoFile);

		AtomicBoolean failIfRemovedFieldsTriggered = new AtomicBoolean(false);

		ProtolockFile protolockFile = getProtolockFile(protoFile);

		if (protolockFile != null) {

			// For each enum on file level (global enums)
			protoFile.types().stream().filter(type -> type instanceof EnumType).map(enumType -> (EnumType) enumType).forEach(enumType -> {
				if (protolockFile.getEnums() != null) {
					Arrays.stream(protolockFile.getEnums()).filter(pe -> pe.getName().equals(enumType.name())).findFirst().ifPresent(protolockEnum -> {
						copyReservations(protolockEnum, enumType);

						if (enumConflictChecker.tryResolveEnumConflicts(protoFile, enumType, protolockEnum)) {
							failIfRemovedFieldsTriggered.set(true);
						}
					});
				}
			});

			protoFile.types().stream().filter(z -> z instanceof MessageType).map(ke -> (MessageType) ke).forEach(e -> {
				// For each root level message in file
				ProtolockMessage protolockMessage = getProtolockMessage(protolockFile, e);
				if (protolockMessage != null) {
					if (resolveBackwardIncompatibilities(protoFile, protolockMessage, e)) {
						failIfRemovedFieldsTriggered.set(true);
					}

				}
			});
		}

		return failIfRemovedFieldsTriggered.get();
	}

	private boolean resolveBackwardIncompatibilities(ProtoFile protoFile, ProtolockMessage protolockMessage, MessageType protoMessage) {
		LOGGER.debug("Resolving backward compabilities in file {}, message {}", protoFile.name(), protoMessage);

		AtomicBoolean failIfRemovedFieldsTriggered = new AtomicBoolean(false);
		// Copy previous reservations since the schema generator has no info about them
		copyReservations(protolockMessage, protoMessage);

		if (fieldConflictChecker.tryResolveFieldConflicts(protoFile, protoMessage, protolockMessage)) {
			failIfRemovedFieldsTriggered.set(true);
		}

		if (tryResolveEnumConflicts(protoFile, protoMessage, protolockMessage)) {
			failIfRemovedFieldsTriggered.set(true);
		}

		protoMessage.nestedTypes().stream().filter(type -> type instanceof MessageType).map(r -> (MessageType) r).forEach(nestedProtoMessage -> {
			ProtolockMessage nestedProtolockMessage = getNestedProtolockMessage(protolockMessage, nestedProtoMessage);
			if (nestedProtolockMessage != null) {
				if (resolveBackwardIncompatibilities(protoFile, nestedProtolockMessage, nestedProtoMessage)) {
					failIfRemovedFieldsTriggered.set(true);
				}
			}
		});

		return failIfRemovedFieldsTriggered.get();

	}

	private boolean tryResolveEnumConflicts(ProtoFile protoFile, MessageType protoMessage, ProtolockMessage protolockMessage) {
		LOGGER.debug("Trying to resolve enum conflicts in file {}, message {}", protoFile.name(), protoMessage);
		AtomicBoolean failIfRemovedFieldsTriggered = new AtomicBoolean(false);
		// For each enum in proto, try to find mismatching enum values and resolve
		protoMessage.nestedTypes().stream().filter(type -> type instanceof EnumType).map(type -> (EnumType) type).forEach(enumType -> {
			// Find matching in protolockmessage
			if (protolockMessage.getEnums() != null) {
				Arrays.stream(protolockMessage.getEnums()).filter(e -> e.getName().equals(enumType.name())).findFirst().ifPresent(protolockEnum -> {
					copyReservations(protolockEnum, enumType);
					if (enumConflictChecker.tryResolveEnumConflicts(protoFile, enumType, protolockEnum)) {
						failIfRemovedFieldsTriggered.set(true);
					}
				});
			}
		});
		return failIfRemovedFieldsTriggered.get();
	}

	private ProtolockMessage getProtolockMessage(ProtolockFile protolockFile, MessageType protoMessage) {
		if (protolockFile != null && protolockFile.getMessages() != null) {
			return Arrays.stream(protolockFile.getMessages()).filter(message -> message.getName().equals(protoMessage.getName())).findFirst().orElse(null);
		}

		return null;
	}

	private ProtolockMessage getNestedProtolockMessage(ProtolockMessage protolockMessage, MessageType nestedProtoMessage) {
		if (protolockMessage != null && protolockMessage.getMessages() != null) {
			return Arrays.stream(protolockMessage.getMessages())
					.filter(subMessage -> subMessage.getName().equals(nestedProtoMessage.getName()))
					.findFirst()
					.orElse(null);
		} else {
			return null;
		}
	}

	private ProtolockFile getProtolockFile(ProtoFile protoFile) {
		String fullPath = protoFile.toString();
		if (!fullPath.contains("/")) {
			// Assume no package in filename yet
			fullPath = protoFile.packageName().replace(".", "/") + "/" + protoFile.toString();
		}

		for (ProtolockDefinition def : definitions.getDefinitions()) {
			if (def.getProtopath().replace(":/:", "/").equals(fullPath)) {
				return def.getFile();
			}
		}

		LOGGER.warn("Could not find a matching entry in proto.lock for {}", protoFile.name());
		return null;
	}

}
