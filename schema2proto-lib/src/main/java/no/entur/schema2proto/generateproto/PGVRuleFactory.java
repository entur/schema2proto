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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squareup.wire.schema.internal.parser.OptionElement;
import com.sun.xml.xsom.XSAttributeDecl;
import com.sun.xml.xsom.XSFacet;
import com.sun.xml.xsom.XSParticle;
import com.sun.xml.xsom.XSRestrictionSimpleType;
import com.sun.xml.xsom.XSSimpleType;

public class PGVRuleFactory {

	public static final String VALIDATE_RULES_NAME = "validate.rules";
	private static final String RULE_STRING_PATTERN = "string.pattern";
	private final SchemaParser schemaParser;
	private Schema2ProtoConfiguration configuration;
	private final Set<String> basicTypes;
	private Map<String, OptionElement> defaultValidationRulesForBasicTypes;
	private static final Logger LOGGER = LoggerFactory.getLogger(PGVRuleFactory.class);

	public PGVRuleFactory(Schema2ProtoConfiguration configuration, SchemaParser schemaParser) {
		this.configuration = configuration;
		this.schemaParser = schemaParser;

		basicTypes = TypeRegistry.getBasicTypes();

		defaultValidationRulesForBasicTypes = new HashMap<>();
		defaultValidationRulesForBasicTypes.putAll(getValidationRuleForBasicTypes());

	}

	public List<OptionElement> getValidationRule(XSParticle parentParticle) {
		List<OptionElement> validationRules = new ArrayList<>();

		if (false && configuration.includeValidationRules) { // TODO SKIP FOR NOW AS RULES ARE NOT COMPILING
			int minOccurs = parentParticle.getMinOccurs() != null ? parentParticle.getMinOccurs().intValue() : 0; // Default
			int maxOccurs = parentParticle.getMaxOccurs() != null && parentParticle.getMaxOccurs().intValue() != 0 ? parentParticle.getMaxOccurs().intValue()
					: 1; // Default

			if (minOccurs == 1 && maxOccurs == 1) {
				validationRules.add(new OptionElement("(validate.rules).message.required", OptionElement.Kind.BOOLEAN, true, false));
			} else if (parentParticle.isRepeated()) {
				Map<String, Integer> minMaxParams = new HashMap<>();
				minMaxParams.put("min_items", minOccurs);
				if (maxOccurs > 1) {
					minMaxParams.put("max_items", maxOccurs);
				}
				validationRules.add(new OptionElement("(validate.rules).repeated", OptionElement.Kind.MAP, minMaxParams, false));
			}

		}
		return validationRules;

	}

	public List<OptionElement> getValidationRule(XSAttributeDecl attributeDecl) {

		List<OptionElement> validationRules = new ArrayList<>();

		if (configuration.includeValidationRules) {
			// TODO
		}
		// TOOD check if optional
		return validationRules;
	}

	public List<OptionElement> getValidationRule(XSSimpleType simpleType) {

		List<OptionElement> validationRules = new ArrayList<>();
		if (configuration.includeValidationRules) {
			String typeName = simpleType.getName();

			if (typeName != null && basicTypes.contains(typeName)) {
				validationRules.addAll(getValidationRuleForBasicType(typeName));
			} else if (simpleType.isRestriction()) {
				XSRestrictionSimpleType restriction = simpleType.asRestriction();
				// XSType baseType = restriction.getBaseType();
				Collection<? extends XSFacet> declaredFacets = restriction.getDeclaredFacets();
				String baseType = schemaParser.findFieldType(simpleType);
				if ("string".equals(baseType)) {
					addOptionForString(validationRules, declaredFacets);
				}

				// TODO check baseType, add restrictions on it.
				// TODO check if facets are inherited or not. If inherited then iterate to top primitive to find
				// base rule, then select supported facets
				// System.out.println("x");
			} else {
				LOGGER.warn("During validation rules extraction; Found anonymous simpleType that is not a restriction: {}", simpleType);

			}

		}
		/*
		 * if (minOccurs == 1 && maxOccurs == 1) {
		 *
		 * OptionElement option = new OptionElement("message.required", OptionElement.Kind.BOOLEAN, true, false); OptionElement e = new
		 * OptionElement(VALIDATE_RULES_NAME, OptionElement.Kind.OPTION, option, true);
		 *
		 * return e; }
		 */

		return validationRules;

	}

	private void addOptionForString(List<OptionElement> validationRules, Collection<? extends XSFacet> declaredFacets) {
		Map<String, Object> parameters = new HashMap<>();
		for (XSFacet facet : declaredFacets) {
			switch (facet.getName()) {
			case XSFacet.FACET_PATTERN:
				parameters.put("pattern", StringUtils.replace(facet.getValue().value, "\\", "\\\\")); // Add escaping of backslash
				break;
			case XSFacet.FACET_MINLENGTH:
				parameters.put("min_len", Integer.parseInt(facet.getValue().value));
				break;
			case XSFacet.FACET_MAXLENGTH:
				parameters.put("max_len", Integer.parseInt(facet.getValue().value));
				break;
			default:
				LOGGER.warn("Unhandled facet {}", facet.getName());
			}
		}
		OptionElement option = new OptionElement("string", OptionElement.Kind.MAP, parameters, false);
		OptionElement e = new OptionElement(VALIDATE_RULES_NAME, OptionElement.Kind.OPTION, option, true);
		validationRules.add(e);
	}

	private List<OptionElement> getValidationRuleForBasicType(String name) {
		List<OptionElement> validationRules = new ArrayList<>();
		OptionElement validationRule = defaultValidationRulesForBasicTypes.get(name);
		if (validationRule != null) {
			validationRules.add(validationRule);

		}
		return validationRules;
	}

	public Map<String, OptionElement> getValidationRuleForBasicTypes() {

		Map<String, OptionElement> typeToValidationRule = new HashMap<>();

//        basicTypes.add("string");
//        basicTypes.add("boolean");
//        basicTypes.add("float");
//        basicTypes.add("double");
//        basicTypes.add("decimal");
//        basicTypes.add("duration");
//        basicTypes.add("dateTime");
//        basicTypes.add("time");
//        basicTypes.add("date");

		typeToValidationRule.put("gYearMonth", createOptionElement(RULE_STRING_PATTERN, OptionElement.Kind.STRING, "[0-9]{4}-[0-9]{2}"));
		typeToValidationRule.put("gYear", createOptionElement(RULE_STRING_PATTERN, OptionElement.Kind.STRING, "[0-9]{4}"));
		typeToValidationRule.put("gMonthDay", createOptionElement(RULE_STRING_PATTERN, OptionElement.Kind.STRING, "[0-9]{4}-[0-9]{2}"));
		typeToValidationRule.put("gDay", createOptionElement(RULE_STRING_PATTERN, OptionElement.Kind.STRING, "[0-9]{2}")); // 1-31
		typeToValidationRule.put("gMonth", createOptionElement(RULE_STRING_PATTERN, OptionElement.Kind.STRING, "[0-9]{2}")); // 1-12

//        basicTypes.add("hexBinary");
//        basicTypes.add("base64Binary");
//        basicTypes.add("anyURI");
//        basicTypes.add("QName");
//        basicTypes.add("NOTATION");
//
//        basicTypes.add("normalizedString");
//        basicTypes.add("token");

		typeToValidationRule.put("language", createOptionElement(RULE_STRING_PATTERN, OptionElement.Kind.STRING, "[a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*"));

//        basicTypes.put("IDREFS");
//        basicTypes.put("ENTITIES");
//        basicTypes.put("NMTOKEN");
//        basicTypes.put("NMTOKENS");
//        basicTypes.put("Name");
//        basicTypes.put("NCName");
//        basicTypes.put("ID");
//        basicTypes.put("IDREF");
//        basicTypes.put("ENTITY");

//        basicTypes.put("integer");

		typeToValidationRule.put("nonPositiveInteger", createOptionElement("sint32.lte", OptionElement.Kind.NUMBER, 0));
		typeToValidationRule.put("negativeInteger", createOptionElement("sint32.lt", OptionElement.Kind.NUMBER, 0));
//        basicTypes.put("long");
//        basicTypes.put("int");
//        basicTypes.put("short");
//        basicTypes.put("byte");

		typeToValidationRule.put("nonNegativeInteger", createOptionElement("uint32.gte", OptionElement.Kind.NUMBER, 0));
//        basicTypes.put("unsignedLong");
//        basicTypes.put("unsignedInt");
//        basicTypes.put("unsignedShort");
//        basicTypes.put("unsignedByte");
		typeToValidationRule.put("positiveInteger", createOptionElement("uint32.gt", OptionElement.Kind.NUMBER, 0));

//        basicTypes.put("anySimpleType");
//        basicTypes.put("anyType");

		return typeToValidationRule;

	}

	private OptionElement createOptionElement(String name, OptionElement.Kind kind, Object value) {
		OptionElement option = new OptionElement(name, kind, value, false);
		return new OptionElement(VALIDATE_RULES_NAME, OptionElement.Kind.OPTION, option, true);
	}

}
