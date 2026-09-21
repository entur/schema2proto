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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.squareup.wire.Syntax;
import com.squareup.wire.schema.Extend;
import com.squareup.wire.schema.Extensions;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.ProtoType;
import com.squareup.wire.schema.Reserved;
import com.squareup.wire.schema.internal.parser.EnumConstantElement;
import com.squareup.wire.schema.internal.parser.EnumElement;
import com.squareup.wire.schema.internal.parser.FieldElement;
import com.squareup.wire.schema.internal.parser.MessageElement;
import com.squareup.wire.schema.internal.parser.OneOfElement;
import com.squareup.wire.schema.internal.parser.OptionElement;
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
		file.weakImports().addAll(element.getWeakImports());
		file.options().optionElements().addAll(escapedOptions(element.getOptions()));
		// Carry extend declarations and services (gRPC RPCs) through unchanged; schema2proto does not modify them.
		file.getExtendList().addAll(protoFile.getExtendList());
		file.getServices().addAll(protoFile.getServices());

		// Namespaces are the scope names used when resolving field types, mirroring com.squareup.wire.schema.Type.fromElements.
		List<String> namespaces = packageName == null ? Collections.emptyList() : Collections.singletonList(packageName);
		for (TypeElement typeElement : element.getTypes()) {
			file.types().add(fromType(typeElement, packageName, namespaces));
		}
		return file;
	}

	private static MutableType fromType(TypeElement typeElement, String enclosing, List<String> namespaces) {
		if (typeElement instanceof MessageElement) {
			return fromMessage((MessageElement) typeElement, enclosing, namespaces);
		} else if (typeElement instanceof EnumElement) {
			return fromEnum((EnumElement) typeElement, enclosing);
		}
		throw new IllegalArgumentException("Unsupported type element: " + typeElement.getClass());
	}

	private static String qualify(String enclosing, String name) {
		return enclosing == null || enclosing.isEmpty() ? name : enclosing + "." + name;
	}

	private static MutableMessageType fromMessage(MessageElement element, String enclosing, List<String> namespaces) {
		String qualified = qualify(enclosing, element.getName());
		MutableOptions options = new MutableOptions(MutableOptions.MESSAGE_OPTIONS, escapedOptions(element.getOptions()));
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
		// Namespaces for all child elements include this message's name, mirroring com.squareup.wire.schema.MessageType.fromElement.
		List<String> childNamespaces = new ArrayList<>(namespaces.isEmpty() ? List.of("") : namespaces);
		childNamespaces.add(element.getName());
		for (TypeElement nested : element.getNestedTypes()) {
			message.nestedTypes().add(fromType(nested, qualified, childNamespaces));
		}
		message.getNestedExtendList().addAll(Extend.fromElements(childNamespaces, element.getExtendDeclarations()));
		message.getExtensionsList().addAll(Extensions.fromElements(element.getExtensions()));
		return message;
	}

	private static MutableEnumType fromEnum(EnumElement element, String enclosing) {
		String qualified = qualify(enclosing, element.getName());
		MutableOptions options = new MutableOptions(MutableOptions.ENUM_OPTIONS, escapedOptions(element.getOptions()));
		List<MutableEnumConstant> constants = new ArrayList<>();
		for (EnumConstantElement constantElement : element.getConstants()) {
			constants.add(new MutableEnumConstant(constantElement.getLocation(), constantElement.getName(), constantElement.getTag(),
					constantElement.getDocumentation(), new MutableOptions(MutableOptions.ENUM_VALUE_OPTIONS, escapedOptions(constantElement.getOptions()))));
		}
		List<Reserved> reserveds = new ArrayList<>();
		for (ReservedElement reservedElement : element.getReserveds()) {
			reserveds.add(fromReserved(reservedElement));
		}
		return new MutableEnumType(ProtoType.get(qualified), element.getLocation(), element.getDocumentation(), element.getName(), constants, reserveds,
				options);
	}

	private static MutableField fromField(FieldElement element) {
		MutableOptions options = new MutableOptions(MutableOptions.FIELD_OPTIONS, escapedOptions(element.getOptions()));
		MutableField field = new MutableField(null, element.getLocation(), element.getLabel(), element.getName(), element.getDocumentation(), element.getTag(),
				element.getDefaultValue(), element.getType(), options, false, false);
		field.setJsonName(element.getJsonName());
		return field;
	}

	private static MutableOneOf fromOneOf(OneOfElement element) {
		MutableOptions options = new MutableOptions(MutableOptions.ONEOF_OPTIONS, escapedOptions(element.getOptions()));
		List<MutableField> fields = new ArrayList<>();
		for (FieldElement fieldElement : element.getFields()) {
			fields.add(fromField(fieldElement));
		}
		return new MutableOneOf(element.getName(), element.getDocumentation(), fields, options);
	}

	/**
	 * The mutable model holds option string values in proto source form, i.e. already escaped: the XSD-to-proto path escapes them as it builds them (see
	 * {@code ValidationRuleFactory}). Wire's parser, however, unescapes as it reads, and its serializer only escapes again for a top level {@code STRING}
	 * option — {@code OptionElement.formatOptionMapValue} appends strings nested in an aggregate (message literal) option verbatim. Values arriving through
	 * that asymmetric path are restored to proto source form here, so a {@code buf.validate} pattern written {@code "x\\-y"} is not emitted as {@code "x\-y"},
	 * which protoc rejects with "Invalid escape sequence in string literal".
	 *
	 * <p>
	 * Only the values wire renders unescaped are touched. A top level {@code STRING}, and the nested elements of {@code OPTION} and {@code LIST} (each rendered
	 * by its own {@code toSchema()}), are left as wire parsed them, so nothing ends up escaped twice.
	 */
	private static List<OptionElement> escapedOptions(List<OptionElement> options) {
		List<OptionElement> result = new ArrayList<>(options.size());
		for (OptionElement option : options) {
			result.add(escapeAggregateStrings(option));
		}
		return result;
	}

	private static OptionElement escapeAggregateStrings(OptionElement option) {
		switch (option.getKind()) {
		case MAP:
			return new OptionElement(option.getName(), option.getKind(), escapeAggregateValue(option.getValue()), option.isParenthesized());
		case OPTION:
			if (option.getValue() instanceof OptionElement nested) {
				return new OptionElement(option.getName(), option.getKind(), escapeAggregateStrings(nested), option.isParenthesized());
			}
			return option;
		case LIST:
			if (option.getValue() instanceof List<?> items) {
				List<Object> escapedItems = new ArrayList<>(items.size());
				for (Object item : items) {
					escapedItems.add(item instanceof OptionElement element ? escapeAggregateStrings(element) : item);
				}
				return new OptionElement(option.getName(), option.getKind(), escapedItems, option.isParenthesized());
			}
			return option;
		default:
			return option;
		}
	}

	/** Escapes every string reachable from an aggregate option value, mirroring the structures {@code formatOptionMapValue} walks. */
	private static Object escapeAggregateValue(Object value) {
		if (value instanceof String string) {
			return escapeQuotedString(string);
		}
		if (value instanceof OptionElement.OptionPrimitive primitive) {
			return switch (primitive.getKind()) {
			case BOOLEAN, NUMBER, ENUM -> primitive;
			default -> new OptionElement.OptionPrimitive(primitive.getKind(), escapeAggregateValue(primitive.getValue()));
			};
		}
		if (value instanceof Map<?, ?> map) {
			Map<Object, Object> escaped = new LinkedHashMap<>();
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				escaped.put(entry.getKey(), escapeAggregateValue(entry.getValue()));
			}
			return escaped;
		}
		if (value instanceof List<?> list) {
			List<Object> escaped = new ArrayList<>(list.size());
			for (Object item : list) {
				escaped.add(escapeAggregateValue(item));
			}
			return escaped;
		}
		return value;
	}

	/** Mirrors {@code OptionElement.escapeQuotedString}, which stock wire applies to top level string options but not to aggregate ones. */
	private static String escapeQuotedString(String value) {
		StringBuilder result = new StringBuilder(value.length());
		for (int i = 0; i < value.length(); i++) {
			char c = value.charAt(i);
			switch (c) {
			case '"' -> result.append("\\\"");
			case '\'' -> result.append("\\'");
			case '\\' -> result.append("\\\\");
			case '\b' -> result.append("\\b");
			case '\n' -> result.append("\\n");
			case '\r' -> result.append("\\r");
			case '\t' -> result.append("\\t");
			case '\u0007' -> result.append("\\a");
			case '\f' -> result.append("\\f");
			case '\u000b' -> result.append("\\v");
			default -> result.append(c);
			}
		}
		return result.toString();
	}

	private static Reserved fromReserved(ReservedElement element) {
		return new Reserved(element.getLocation(), element.getDocumentation(), element.getValues());
	}
}
