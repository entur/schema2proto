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
package no.entur.schema2proto.generateproto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TypeAndNameMapper {
	private Map<Pattern, String> typeMappings = new LinkedHashMap<>();
	private Map<Pattern, String> typeReplacing = new LinkedHashMap<>();
	private Map<Pattern, String> nameMappings = new LinkedHashMap<>();
	private Set<String> reservedJavaKeywords = new HashSet<>();
	private List<FieldPath> ignoreFieldPaths;

	public TypeAndNameMapper(Schema2ProtoConfiguration configuration) {
		typeReplacing.putAll(getStandardXsdTypeMappings());
		// From external
		updateMappings(typeReplacing, configuration.customTypeReplacements);

		// From external configuration
		updateMappings(typeMappings, configuration.customTypeMappings);

		// From external configuration
		updateMappings(nameMappings, configuration.customNameMappings);

		reservedJavaKeywords.addAll(getReservedWords());

		this.ignoreFieldPaths = configuration.ignoreOutputFields;

	}

	private void updateMappings(Map<Pattern, String> existing, Map<Pattern, String> updated) {
		// Compiled patterns cannot be compared using equals(!)

		for (Pattern updatedKey : updated.keySet()) {
			Set<Pattern> keys = new HashSet<>(existing.keySet());
			for (Pattern key : keys) {
				if (key.pattern().equals(updatedKey.pattern())) {
					existing.remove(key); // Or it will remain
				}
			}
			existing.put(updatedKey, updated.get(updatedKey));
		}

	}

	private Map<Pattern, String> getStandardXsdTypeMappings() {
		Map<Pattern, String> standardTypeMappings = new HashMap<>();

		// From https://www.w3.org/TR/xmlschema-2/#d0e11239.
		// Same order as defined above

		standardTypeMappings.put(Pattern.compile("^string$"), "string");
		standardTypeMappings.put(Pattern.compile("^boolean$"), "bool");
		standardTypeMappings.put(Pattern.compile("^float$"), "float");
		standardTypeMappings.put(Pattern.compile("^double$"), "double");
		standardTypeMappings.put(Pattern.compile("^decimal$"), "double");
		standardTypeMappings.put(Pattern.compile("^duration$"), "string"); // XML duration string
		standardTypeMappings.put(Pattern.compile("^dateTime$"), "uint64"); // Number of milliseconds since January 1st), 1970
		standardTypeMappings.put(Pattern.compile("^time$"), "uint64"); // Number of milliseconds since midnight
		standardTypeMappings.put(Pattern.compile("^date$"), "uint32"); // Number of days since January 1st 1970

		standardTypeMappings.put(Pattern.compile("^gYearMonth$"), "string");
		standardTypeMappings.put(Pattern.compile("^gYear$"), "uint32");
		standardTypeMappings.put(Pattern.compile("^gMonthDay$"), "string");
		standardTypeMappings.put(Pattern.compile("^gDay$"), "uint32");
		standardTypeMappings.put(Pattern.compile("^gMonth$"), "uint32");

		standardTypeMappings.put(Pattern.compile("^hexBinary$"), "bytes");
		standardTypeMappings.put(Pattern.compile("^base64Binary$"), "bytes");
		standardTypeMappings.put(Pattern.compile("^anyURI$"), "string");
		standardTypeMappings.put(Pattern.compile("^QName$"), "string");
		standardTypeMappings.put(Pattern.compile("^NOTATION$"), "string"); // Unsure

		standardTypeMappings.put(Pattern.compile("^normalizedString$"), "string");
		standardTypeMappings.put(Pattern.compile("^token$"), "string");
		standardTypeMappings.put(Pattern.compile("^language$"), "string");

		standardTypeMappings.put(Pattern.compile("^IDREFS$"), "string");
		standardTypeMappings.put(Pattern.compile("^ENTITIES$"), "string");
		standardTypeMappings.put(Pattern.compile("^NMTOKEN$"), "string");
		standardTypeMappings.put(Pattern.compile("^NMTOKENS$"), "string"); // TODO: Fix this should be repeated string
		standardTypeMappings.put(Pattern.compile("^Name$"), "string");
		standardTypeMappings.put(Pattern.compile("^NCName$"), "string");
		standardTypeMappings.put(Pattern.compile("^ID$"), "string");
		standardTypeMappings.put(Pattern.compile("^IDREF$"), "string");
		standardTypeMappings.put(Pattern.compile("^ENTITY$"), "string");

		standardTypeMappings.put(Pattern.compile("^integer$"), "int32");
		standardTypeMappings.put(Pattern.compile("^nonPositiveInteger$"), "sint32");
		standardTypeMappings.put(Pattern.compile("^negativeInteger$"), "sint32");
		standardTypeMappings.put(Pattern.compile("^long$"), "int64");
		standardTypeMappings.put(Pattern.compile("^int$"), "int32");
		standardTypeMappings.put(Pattern.compile("^short$"), "int32"); // No 16-bit int in protobuf
		standardTypeMappings.put(Pattern.compile("^byte$"), "bytes");

		standardTypeMappings.put(Pattern.compile("^nonNegativeInteger$"), "uint32");
		standardTypeMappings.put(Pattern.compile("^unsignedLong$"), "uint64");
		standardTypeMappings.put(Pattern.compile("^unsignedInt$"), "uint32");
		standardTypeMappings.put(Pattern.compile("^unsignedShort$"), "uint32"); // No 16-bit int in protobuf
		standardTypeMappings.put(Pattern.compile("^unsignedByte$"), "uint32"); // No 8-bit int in protobuf
		standardTypeMappings.put(Pattern.compile("^positiveInteger$"), "uint32");

		standardTypeMappings.put(Pattern.compile("^anySimpleType$"), "string"); // base type of all primitive types
		standardTypeMappings.put(Pattern.compile("^anyType$"), "string"); // Wildcard

		return standardTypeMappings;
	}

	public String translateType(String type) {

		for (Map.Entry<Pattern, String> p : typeMappings.entrySet()) {
			Matcher m = p.getKey().matcher(type);
			if (m.matches()) {
				type = m.replaceAll(p.getValue());
				break;
			}
		}

		type = type.replace("-", "");

		return type;

	}

	public String translateFieldName(String name) {
		for (Pattern p : nameMappings.keySet()) {
			Matcher m = p.matcher(name);
			if (m.matches()) {
				return m.replaceAll(nameMappings.get(p));
			}
		}

		return name;
	}

	public String escapeFieldName(String fieldName) {
		if (reservedJavaKeywords.contains(fieldName)) {
			return fieldName + "_field";
		} else {
			return fieldName;
		}
	}

	public boolean ignoreOutputField(String packageName, String messageName, String fieldName) {
		for (FieldPath f : ignoreFieldPaths) {
			if (f.matches(packageName, messageName, fieldName)) {
				return true;
			}
		}

		return false;
	}

	private Set<String> getReservedWords() {

		Set<String> reservedWords = new HashSet<>();
		reservedWords.add("abstract");
		reservedWords.add("assert");
		reservedWords.add("boolean");
		reservedWords.add("break");
		reservedWords.add("byte");
		reservedWords.add("case");
		reservedWords.add("catch");
		reservedWords.add("char");
		reservedWords.add("class");
		reservedWords.add("const");
		reservedWords.add("default");

		reservedWords.add("do");
		reservedWords.add("double");
		reservedWords.add("else");
		reservedWords.add("enum");
		reservedWords.add("extends");
		reservedWords.add("false");
		reservedWords.add("final");
		reservedWords.add("finally");
		reservedWords.add("float");
		reservedWords.add("for");
		reservedWords.add("goto");

		reservedWords.add("if");
		reservedWords.add("implements");
		reservedWords.add("import");
		reservedWords.add("instanceof");
		reservedWords.add("int");
		reservedWords.add("interface");
		reservedWords.add("long");
		reservedWords.add("native");
		reservedWords.add("new");
		reservedWords.add("null");
		reservedWords.add("package");

		reservedWords.add("private");
		reservedWords.add("protected");
		reservedWords.add("public");
		reservedWords.add("return");
		reservedWords.add("short");
		reservedWords.add("static");
		reservedWords.add("strictfp");
		reservedWords.add("super");
		reservedWords.add("switch");
		reservedWords.add("synchronized");

		reservedWords.add("this");
		reservedWords.add("throw");
		reservedWords.add("throws");
		reservedWords.add("transient");
		reservedWords.add("true");
		reservedWords.add("try");
		reservedWords.add("void");
		reservedWords.add("volatile");
		reservedWords.add("while");
		reservedWords.add("continue");

		return reservedWords;
	}

	public String replaceType(String type) {
		for (Map.Entry<Pattern, String> p : typeReplacing.entrySet()) {
			Matcher m = p.getKey().matcher(type);
			if (m.matches()) {
				type = m.replaceAll(p.getValue());
				break;
			}
		}

		return type;

	}
}
