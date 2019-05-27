package no.entur.schema2proto.marshal;

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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.CaseFormat;

import no.entur.schema2proto.NamespaceConverter;
import no.entur.schema2proto.Schema2ProtoConfiguration;
import no.entur.schema2proto.proto.ProtobufField;

public class ProtobufMarshaller {
	private HashMap<Pattern, String> typeMapping = new HashMap<>();
	private HashMap<Pattern, String> nameMapping = new HashMap<>();
	private HashMap<String, String> imports = new HashMap<>();
	private Map<String, String> options = new HashMap<>();
	private String indent = "";

	public ProtobufMarshaller(Schema2ProtoConfiguration configuration) {
		typeMapping.put(Pattern.compile("^positiveInteger$"), "int64");
		typeMapping.put(Pattern.compile("^nonPositiveInteger$"), "sint64");
		typeMapping.put(Pattern.compile("^negativeInteger$"), "sint64");
		typeMapping.put(Pattern.compile("^nonNegativeInteger$"), "int64");
		typeMapping.put(Pattern.compile("^int$"), "int32");
		typeMapping.put(Pattern.compile("^integer$"), "int64");
		typeMapping.put(Pattern.compile("^unsignedLong$"), "uint64");
		typeMapping.put(Pattern.compile("^unsignedInt$"), "uint32");
		typeMapping.put(Pattern.compile("^unsignedShort$"), "uint32"); // No 16-bit int in protobuf
		typeMapping.put(Pattern.compile("^unsignedByte$"), "uint32"); // No 8-bit int in protobuf
		typeMapping.put(Pattern.compile("^short$"), "int32"); // No 16-bit int in protobuf
		typeMapping.put(Pattern.compile("^long$"), "int64");
		typeMapping.put(Pattern.compile("^decimal$"), "double");
		typeMapping.put(Pattern.compile("^ID$"), "string");
		typeMapping.put(Pattern.compile("^Name$"), "string");
		typeMapping.put(Pattern.compile("^IDREF$"), "string");
		typeMapping.put(Pattern.compile("^NMTOKEN$"), "string");
		typeMapping.put(Pattern.compile("^NMTOKENS$"), "string"); // TODO: Fix this
		typeMapping.put(Pattern.compile("^anySimpleType$"), "string");
		typeMapping.put(Pattern.compile("^anyType$"), "string");
		typeMapping.put(Pattern.compile("^anyURI$"), "string");
		typeMapping.put(Pattern.compile("^normalizedString$"), "string");
		typeMapping.put(Pattern.compile("^boolean$"), "bool");
		typeMapping.put(Pattern.compile("^binary$"), "bytes"); // UnspecifiedType.object is
		typeMapping.put(Pattern.compile("^hexBinary$"), "bytes");
		typeMapping.put(Pattern.compile("^base64Binary$"), "bytes");
		typeMapping.put(Pattern.compile("^byte$"), "bytes");
		typeMapping.put(Pattern.compile("^date$"), "int32"); // Number of days since January 1st 1970
		typeMapping.put(Pattern.compile("^dateTime$"), "int64"); // Number of milliseconds since January 1st), 1970

		typeMapping.put(Pattern.compile("^time$"), "google.protobuf.Timestamp");
		typeMapping.put(Pattern.compile("^duration$"), "google.protobuf.Duration");

		imports.put("google.protobuf.Timestamp", "google/protobuf/timestamp");
		imports.put("google.protobuf.Duration", "google/protobuf/duration");

		// From external configuration
		typeMapping.putAll(configuration.customTypeMappings);
		nameMapping.putAll(configuration.customNameMappings);
		options.putAll(configuration.options);
	}

	public String writeHeader(String namespace) {

		StringBuilder b = new StringBuilder();

		// Syntax
		b.append("syntax = \"proto3\";\n\n");

		if (namespace != null) {
			b.append("package ");
			b.append(escapeNamespace(namespace));
			b.append(";\n\n");
		}

		if (options != null) {
			for (String s : options.keySet()) {

				b.append("option ").append(s).append(" = ");

				Object value = options.get(s);

				if (value instanceof String) {
					b.append("\"").append(value).append("\"");
				} else {
					b.append(value);
				}

				b.append(";\n");
			}
		}

		return b.toString();
	}

	public String escapeNamespace(String namespace) {
		if (namespace == null) {
			return null;
		}
		return namespace.replaceAll("\\.([0-9])", "_$1");
	}

	public String writeEnumHeader(String name) {
		final String result = writeIndent() + "enum " + name + "\n" + writeIndent() + "{\n";
		increaseIndent();
		return result;
	}

	public String writeEnumValue(int order, String value) {
		return (writeIndent() + CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, value) + " = " + order + ";\n");
	}

	public String writeEnumFooter() {
		decreaseIndent();
		return writeIndent() + "}\n";
	}

	public String writeStructHeader(String name) {
		final String result = writeIndent() + "message " + name + "\n{\n";
		increaseIndent();
		return result;
	}

	public String writeStructParameter(int order, boolean repeated, String name, String type, String fieldDocumentation, boolean splitByNamespace) {
		String modifier = "";

		if (fieldDocumentation != null) {
			fieldDocumentation = fieldDocumentation.replaceAll("\n", " ").replaceAll("\t", " ");
		}

		if (repeated) {
			modifier = "repeated ";
		}
		String fieldName = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, name);

		String convertedType = NamespaceConverter.convertFromSchema(type);

		if (imports.containsKey(type)) {
			convertedType = type;
		} else if (!splitByNamespace) {
			convertedType = convertedType.substring(convertedType.lastIndexOf(".") + 1);
		}

		return writeIndent() + modifier + convertedType + " " + fieldName + " = " + order + ";"
				+ (fieldDocumentation != null ? " // " + fieldDocumentation : "") + "\n";
	}

	public String writeStructFooter() {
		decreaseIndent();
		return writeIndent() + "}\n\n";
	}

	public String getTypeMapping(String type) {
		for (Pattern p : typeMapping.keySet()) {
			Matcher m = p.matcher(type);
			if (m.find()) {
				return m.replaceAll(typeMapping.get(p));
			}
		}

		return null;
	}

	public String getNameMapping(String type) {
		for (Pattern p : nameMapping.keySet()) {
			Matcher m = p.matcher(type);
			if (m.find()) {
				return m.replaceAll(nameMapping.get(p));
			}
		}

		return null;
	}

	public boolean isCircularDependencySupported() {
		return true;
	}

	private void increaseIndent() {
		indent += "  ";
	}

	private void decreaseIndent() {
		indent = indent.substring(0, indent.length() > 0 ? indent.length() - 2 : 0);
	}

	private String writeIndent() {
		return indent;
	}

	public String writeInclude(String namespace) {
		String res;

		if (namespace != null && !namespace.isEmpty()) {
			res = "import \"" + namespace + ".proto\";\n";
		} else {
			res = "";
		}

		return res;
	}

	public String getImport(String fullTypeName) {
		if (imports != null) {
			return imports.get(fullTypeName);
		}
		return null;
	}

	public String getImport(ProtobufField field) {
		if (imports != null) {
			String nsPrefix = "";
			if (field.getTypePackage() != null) {
				nsPrefix = field.getTypePackage() + ".";
			}
			return imports.get(nsPrefix + field.getType());
		}
		return null;
	}

	public Map<String, String> getImports() {
		return imports;
	}

}
