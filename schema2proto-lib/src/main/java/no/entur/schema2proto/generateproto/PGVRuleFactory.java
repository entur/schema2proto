package no.entur.schema2proto.generateproto;

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

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.squareup.wire.schema.internal.parser.OptionElement;
import com.sun.xml.xsom.*;

public class PGVRuleFactory {

	public static final String VALIDATE_RULES_NAME = "validate.rules";
	private final SchemaParser schemaParser;
	private Schema2ProtoConfiguration configuration;
	private final Set<String> basicTypes;
	private Map<String, OptionElement> defaultValidationRulesForBasicTypes;

	private static final Logger LOGGER = LoggerFactory.getLogger(PGVRuleFactory.class);
	private final TypeAndNameMapper typeMapper;

	public PGVRuleFactory(Schema2ProtoConfiguration configuration, SchemaParser schemaParser) {
		this.configuration = configuration;
		this.schemaParser = schemaParser;

		basicTypes = TypeRegistry.getBasicTypes();

		defaultValidationRulesForBasicTypes = new HashMap<>();
		defaultValidationRulesForBasicTypes.putAll(getValidationRuleForBasicTypes());
		typeMapper = new TypeAndNameMapper(configuration);

	}

	public OptionElement getValidationRule(XSParticle parentParticle, boolean isComplexType) {

		if (configuration.includeValidationRules && parentParticle != null) {

			int minOccurs = 0; // Default
			int maxOccurs = 1; // Default

			if (parentParticle.getMinOccurs() != null) {
				minOccurs = parentParticle.getMinOccurs().intValue();
			}

			if (parentParticle.getMaxOccurs() != null) {
				maxOccurs = parentParticle.getMaxOccurs().intValue();
			}

			if (minOccurs == 0 && maxOccurs == 1) {
				// Default case
			} else {
				if (minOccurs == 1 && maxOccurs == 1 && isComplexType) {
					OptionElement option = new OptionElement("message.required", OptionElement.Kind.BOOLEAN, true, false);
					OptionElement e = new OptionElement(VALIDATE_RULES_NAME, OptionElement.Kind.OPTION, option, true);
					return e;
				} else {
					Map<String, Object> parameters = new HashMap<>();
					parameters.put("min_items", minOccurs);
					if (maxOccurs != -1) {
						parameters.put("max_items", maxOccurs);
					}
					OptionElement option = new OptionElement("repeated", OptionElement.Kind.MAP, parameters, false);
					OptionElement e = new OptionElement(VALIDATE_RULES_NAME, OptionElement.Kind.OPTION, option, true);

					return e;
				}
			}

		}

		return null;

	}

	public List<OptionElement> getValidationRule(XSParticle parentParticle, XSAttributeDecl attributeDecl) {

		List<OptionElement> validationRules = new ArrayList<>();

		if (configuration.includeValidationRules) {
			validationRules.addAll(getValidationRule(parentParticle, attributeDecl.getType()));
		}
		return validationRules;
	}

	public List<OptionElement> getValidationRule(XSParticle parentParticle, XSSimpleType simpleType) {

		List<OptionElement> validationRules = new ArrayList<>();
		if (configuration.includeValidationRules) {
			String typeName = simpleType.getName();

			if (typeName != null && basicTypes.contains(typeName)) {
				validationRules.addAll(getValidationRuleForBasicType(typeName));
			}

			if (simpleType.isRestriction() && simpleType.getFacet("enumeration") != null) {
				Map<String, Object> parameters = new HashMap<>();
				parameters.put("defined_only", true);
				OptionElement option = new OptionElement("enum", OptionElement.Kind.MAP, parameters, false);
				OptionElement e = new OptionElement(VALIDATE_RULES_NAME, OptionElement.Kind.OPTION, option, true);
				validationRules.add(e);
			} else if (simpleType.isRestriction()) {

				XSRestrictionSimpleType restriction = simpleType.asRestriction();
				Collection<? extends XSFacet> declaredFacets = restriction.getDeclaredFacets();
				// TODO handle List and Union
				while (declaredFacets.size() == 0 && simpleType.getBaseType() != simpleType && !simpleType.getBaseType().asSimpleType().isList()
						&& !simpleType.getBaseType().asSimpleType().isUnion()) {
					simpleType = (XSSimpleType) simpleType.getBaseType();
					restriction = simpleType.asRestriction();
					declaredFacets = restriction.getDeclaredFacets();
				}

				if (declaredFacets.size() > 0) {
					String baseType = schemaParser.findFieldType(simpleType);
					String protobufType = typeMapper.replaceType(baseType);
					Map<String, Object> parameters = new HashMap<>();
					switch (protobufType) {
					case "string": {
						for (XSFacet facet : declaredFacets) {
							switch (facet.getName()) {
							case "pattern":
								parameters.put("pattern", facet.getValue().value);
								break;
							case "minLength":
								parameters.put("min_len", facet.getValue().value);
								break;
							case "maxLength":
								parameters.put("max_len", facet.getValue().value);
								break;
							case "whiteSpace":
								break;
							default:
								LOGGER.warn("Unhandled string facet when generating validation rules: " + facet.getName());
							}
						}

						break;
					}
					case "double":
					case "int32":
					case "int64":
					case "uint32":
					case "uint64":
					case "sint32":
					case "sint64":
					case "fixed32":
					case "fixed64":
					case "sfixed32":
					case "sfixed64":
					case "float": {
						for (XSFacet facet : declaredFacets) {
							switch (facet.getName()) {
							case "maxInclusive":
								parameters.put("lte", facet.getValue().value);
								break;
							case "maxExclusive":
								parameters.put("lt", facet.getValue().value);
								break;
							case "minInclusive":
								parameters.put("gte", facet.getValue().value);
								break;
							case "inExclusive":
								parameters.put("gt", facet.getValue().value);
								break;

							// Not supported
							case "whiteSpace":
							case "fractionDigits":
								break;
							default:
								LOGGER.warn("Unhandled number facet when generating validation rules: " + facet.getName());
							}
						}

						break;
					}
					case "bool":
						break;

					default: {
						LOGGER.info("Not generating validation rules for restriction on " + baseType);
					}
					}

					OptionElement repeatValidationRule = getValidationRule(parentParticle, false);

					if (!parameters.isEmpty()) {
						// Repeat rule with items
						String prefix = "";
						if (repeatValidationRule != null) {
							prefix = "repeated.items.";
						}
						OptionElement option = new OptionElement(prefix + protobufType, OptionElement.Kind.MAP, parameters, false);
						OptionElement e = new OptionElement(VALIDATE_RULES_NAME, OptionElement.Kind.OPTION, option, true);
						validationRules.add(e);
					}

					if (repeatValidationRule != null) {
						validationRules.add(repeatValidationRule);
					}

					// TODO check baseType, add restrictions on it.
					// TODO check if facets are inherited or not. If inherited then iterate to top primitive to find
					// base rule, then select supported facets
					// System.out.println("x");
				}
			} else if (simpleType.isUnion()) {
				LOGGER.warn("Handling of validation rules for unions is not implemented");
			} else if (simpleType.isList()) {
				LOGGER.warn("Handling of validation rules for list is not implemented");
			} else {
				LOGGER.warn("During validation rules extraction; Found anonymous simpleType that is not a restriction", simpleType);

			}

		}
		return validationRules;

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

		Map<String, OptionElement> basicTypes = new HashMap<>();

//        basicTypes.add("string");
//        basicTypes.add("boolean");
//        basicTypes.add("float");
//        basicTypes.add("double");
//        basicTypes.add("decimal");
//        basicTypes.add("duration");
//        basicTypes.add("dateTime");
//        basicTypes.add("time");
//        basicTypes.add("date");

		basicTypes.put("gYearMonth", createOptionElement("string.pattern", OptionElement.Kind.STRING, "[0-9]{4}-[0-9]{2}"));
		basicTypes.put("gYear", createOptionElement("string.pattern", OptionElement.Kind.STRING, "[0-9]{4}"));
		basicTypes.put("gMonthDay", createOptionElement("string.pattern", OptionElement.Kind.STRING, "[0-9]{4}-[0-9]{2}"));
		basicTypes.put("gDay", createOptionElement("string.pattern", OptionElement.Kind.STRING, "[0-9]{2}")); // 1-31
		basicTypes.put("gMonth", createOptionElement("string.pattern", OptionElement.Kind.STRING, "[0-9]{2}")); // 1-12

//        basicTypes.add("hexBinary");
//        basicTypes.add("base64Binary");
//        basicTypes.add("anyURI");
//        basicTypes.add("QName");
//        basicTypes.add("NOTATION");
//
//        basicTypes.add("normalizedString");
//        basicTypes.add("token");

		basicTypes.put("language", createOptionElement("string.pattern", OptionElement.Kind.STRING, "[a-zA-Z]{1,8}(-[a-zA-Z0-9]{1,8})*"));

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

		basicTypes.put("nonPositiveInteger", createOptionElement("sint32.lte", OptionElement.Kind.NUMBER, 0));
		basicTypes.put("negativeInteger", createOptionElement("sint32.lt", OptionElement.Kind.NUMBER, 0));
//        basicTypes.put("long");
//        basicTypes.put("int");
//        basicTypes.put("short");
//        basicTypes.put("byte");

		basicTypes.put("nonNegativeInteger", createOptionElement("uint32.gte", OptionElement.Kind.NUMBER, 0));
//        basicTypes.put("unsignedLong");
//        basicTypes.put("unsignedInt");
//        basicTypes.put("unsignedShort");
//        basicTypes.put("unsignedByte");
		basicTypes.put("positiveInteger", createOptionElement("uint32.gte", OptionElement.Kind.NUMBER, 1));

//        basicTypes.put("anySimpleType");
//        basicTypes.put("anyType");

		return basicTypes;

	}

	private OptionElement createOptionElement(String name, OptionElement.Kind kind, Object value) {
		OptionElement option = new OptionElement(name, kind, value, false);
		OptionElement wrapper = new OptionElement(VALIDATE_RULES_NAME, OptionElement.Kind.OPTION, option, true);

		return wrapper;
	}
}
