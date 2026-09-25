/*-
 * #%L
 * schema2proto-wire
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.ProtoType;
import com.squareup.wire.schema.Schema;

/**
 * A linked schema in the mutable model: every proto file of a stock wire {@link Schema} converted with {@link WireBuilders#fromProtoFile}, with each field's
 * resolved type ({@link MutableField#type()}) and declaring package ({@link MutableField#packageName()}) taken from the linked schema.
 *
 * <p>
 * This is the combination the vendored wire fork offered, and which stock wire does not: a model that can both be navigated by resolved type and modified. The
 * mutable types are snapshots taken at construction; modifying them does not update the underlying {@link #schema()}.
 */
public final class MutableSchema {

	private final Schema schema;
	private final List<MutableProtoFile> protoFiles = new ArrayList<>();
	private final Map<String, MutableProtoFile> protoFilesByPath = new HashMap<>();
	private final Map<ProtoType, MutableType> types = new HashMap<>();

	private MutableSchema(Schema schema) {
		this.schema = schema;
		for (ProtoFile protoFile : schema.getProtoFiles()) {
			MutableProtoFile mutableProtoFile = WireBuilders.fromProtoFile(protoFile);
			protoFiles.add(mutableProtoFile);
			protoFilesByPath.put(protoFile.getLocation().getPath(), mutableProtoFile);
			index(mutableProtoFile.types());
		}
	}

	public static MutableSchema from(Schema schema) {
		return new MutableSchema(schema);
	}

	private void index(List<MutableType> mutableTypes) {
		for (MutableType mutableType : mutableTypes) {
			types.put(mutableType.type(), mutableType);
			if (mutableType instanceof MutableMessageType mutableMessageType) {
				MessageType linkedMessageType = (MessageType) schema.getType(mutableMessageType.type());
				for (MutableField mutableField : mutableMessageType.fieldsAndOneOfFields()) {
					Field linkedField = linkedMessageType.field(mutableField.name());
					if (linkedField != null) {
						mutableField.setType(linkedField.getType());
						mutableField.updatePackageName(linkedField.getPackageName());
					}
				}
			}
			index(mutableType.nestedTypes());
		}
	}

	/** The linked stock wire schema this was built from. */
	public Schema schema() {
		return schema;
	}

	public List<MutableProtoFile> protoFiles() {
		return protoFiles;
	}

	/**
	 * @param path the file's path relative to its source root, e.g. {@code foo/bar.proto}
	 * @return the file, or null if the schema has no file at that path
	 */
	public MutableProtoFile protoFile(String path) {
		return protoFilesByPath.get(WireSchemaLoader.normalizePath(path));
	}

	/** @return the message or enum type, or null for scalar and unknown types */
	public MutableType getType(ProtoType protoType) {
		return types.get(protoType);
	}

	/** @return the message or enum type, or null for scalar and unknown types */
	public MutableType getType(String fullyQualifiedName) {
		return getType(ProtoType.get(fullyQualifiedName));
	}
}
