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

import com.squareup.wire.Syntax;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.ProtoType;
import com.squareup.wire.schema.Reserved;
import com.squareup.wire.schema.internal.parser.EnumConstantElement;
import com.squareup.wire.schema.internal.parser.EnumElement;
import com.squareup.wire.schema.internal.parser.FieldElement;
import com.squareup.wire.schema.internal.parser.MessageElement;
import com.squareup.wire.schema.internal.parser.OneOfElement;
import com.squareup.wire.schema.internal.parser.ProtoFileElement;
import com.squareup.wire.schema.internal.parser.ReservedElement;
import com.squareup.wire.schema.internal.parser.TypeElement;

/**
 * Converts stock immutable wire types (via their element AST, {@link ProtoFile#toElement()}) into the mutable builder model used by schema2proto's
 * post-processing and backwards-compatibility logic. Used by the proto-modification path, which loads existing protos with stock wire and then edits them.
 */
public final class WireBuilders {

	private WireBuilders() {
	}

	public static MutableProtoFile fromProtoFile(ProtoFile protoFile) {
		ProtoFileElement element = protoFile.toElement();
		Syntax syntax = element.getSyntax() != null ? element.getSyntax() : Syntax.PROTO_2;
		String packageName = element.getPackageName();

		MutableProtoFile file = new MutableProtoFile(syntax, packageName);
		file.setLocation(element.getLocation());
		file.imports().addAll(element.getImports());
		file.publicImports().addAll(element.getPublicImports());
		file.options().getOptionElements().addAll(element.getOptions());
		// Carry extend declarations and services (gRPC RPCs) through unchanged; schema2proto does not modify them.
		file.getExtendList().addAll(protoFile.getExtendList());
		file.getServices().addAll(protoFile.getServices());

		for (TypeElement typeElement : element.getTypes()) {
			file.types().add(fromType(typeElement, packageName));
		}
		return file;
	}

	private static MutableType fromType(TypeElement typeElement, String enclosing) {
		if (typeElement instanceof MessageElement) {
			return fromMessage((MessageElement) typeElement, enclosing);
		} else if (typeElement instanceof EnumElement) {
			return fromEnum((EnumElement) typeElement, enclosing);
		}
		throw new IllegalArgumentException("Unsupported type element: " + typeElement.getClass());
	}

	private static String qualify(String enclosing, String name) {
		return enclosing == null || enclosing.isEmpty() ? name : enclosing + "." + name;
	}

	private static MutableMessageType fromMessage(MessageElement element, String enclosing) {
		String qualified = qualify(enclosing, element.getName());
		MutableOptions options = new MutableOptions(MutableOptions.MESSAGE_OPTIONS, new ArrayList<>(element.getOptions()));
		MutableMessageType message = new MutableMessageType(ProtoType.get(qualified), element.getLocation(), element.getDocumentation(), element.getName(),
				options);

		for (FieldElement fieldElement : element.getFields()) {
			message.addField(fromField(fieldElement));
		}
		for (OneOfElement oneOfElement : element.getOneOfs()) {
			message.oneOfs().add(fromOneOf(oneOfElement));
		}
		for (ReservedElement reservedElement : element.getReserveds()) {
			message.getReserveds().add(fromReserved(reservedElement));
		}
		for (TypeElement nested : element.getNestedTypes()) {
			message.nestedTypes().add(fromType(nested, qualified));
		}
		return message;
	}

	private static MutableEnumType fromEnum(EnumElement element, String enclosing) {
		String qualified = qualify(enclosing, element.getName());
		MutableOptions options = new MutableOptions(MutableOptions.ENUM_OPTIONS, new ArrayList<>(element.getOptions()));
		List<MutableEnumConstant> constants = new ArrayList<>();
		for (EnumConstantElement constantElement : element.getConstants()) {
			constants.add(new MutableEnumConstant(constantElement.getLocation(), constantElement.getName(), constantElement.getTag(),
					constantElement.getDocumentation(), new MutableOptions(MutableOptions.ENUM_VALUE_OPTIONS, new ArrayList<>(constantElement.getOptions()))));
		}
		List<Reserved> reserveds = new ArrayList<>();
		for (ReservedElement reservedElement : element.getReserveds()) {
			reserveds.add(fromReserved(reservedElement));
		}
		return new MutableEnumType(ProtoType.get(qualified), element.getLocation(), element.getDocumentation(), element.getName(), constants, reserveds,
				options);
	}

	private static MutableField fromField(FieldElement element) {
		MutableOptions options = new MutableOptions(MutableOptions.FIELD_OPTIONS, new ArrayList<>(element.getOptions()));
		return new MutableField(null, element.getLocation(), element.getLabel(), element.getName(), element.getDocumentation(), element.getTag(),
				element.getDefaultValue(), element.getType(), options, false, false);
	}

	private static MutableOneOf fromOneOf(OneOfElement element) {
		MutableOptions options = new MutableOptions(MutableOptions.ONEOF_OPTIONS, new ArrayList<>(element.getOptions()));
		List<MutableField> fields = new ArrayList<>();
		for (FieldElement fieldElement : element.getFields()) {
			fields.add(fromField(fieldElement));
		}
		return new MutableOneOf(element.getName(), element.getDocumentation(), fields, options);
	}

	private static Reserved fromReserved(ReservedElement element) {
		return new Reserved(element.getLocation(), element.getDocumentation(), element.getValues());
	}
}
