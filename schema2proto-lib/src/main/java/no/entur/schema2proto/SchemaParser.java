package no.entur.schema2proto;

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

import java.io.IOException;
import java.util.*;

import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.squareup.wire.schema.EnumConstant;
import com.squareup.wire.schema.EnumType;
import com.squareup.wire.schema.Extensions;
import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.Field.Label;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.OneOf;
import com.squareup.wire.schema.Options;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.ProtoFile.Syntax;
import com.squareup.wire.schema.ProtoType;
import com.squareup.wire.schema.Reserved;
import com.squareup.wire.schema.Type;
import com.squareup.wire.schema.internal.parser.OptionElement;
import com.sun.xml.xsom.*;
import com.sun.xml.xsom.parser.XSOMParser;
import com.sun.xml.xsom.util.DomAnnotationParserFactory;

public class SchemaParser implements ErrorHandler {

	public static final String TYPE_SUFFIX = "Type";
	public static final String GENERATED_NAME_SUFFIX_UNIQUENESS = "GeneratedBySchemaToProto";

	private static final Logger LOGGER = LoggerFactory.getLogger(SchemaParser.class);

	private static final String DEFAULT_PROTO_PACKAGE = "default";
	private static final String WRAPPER_PREFIX = "ChoiceWrapper";

	private Map<String, ProtoFile> packageToProtoFileMap = new HashMap<>();

	private Map<String, String> simpleTypes;
	private Map<String, String> documentation;
	private Map<MessageType, Set<Object>> elementDeclarationsPerMessageType = new HashMap<>();
	private Set<String> basicTypes;

	private Set<String> generatedTypeNames = new HashSet<>();

	private int nestlevel = 0;

	private Schema2ProtoConfiguration configuration;

	private PGVRuleFactory ruleFactory;

	private void init() {
		simpleTypes = new HashMap<String, String>();
		documentation = new HashMap<String, String>();

		basicTypes = new TreeSet<String>();
		basicTypes.addAll(TypeRegistry.getBasicTypes());

		ruleFactory = new PGVRuleFactory(configuration, this);

	}

	public SchemaParser(Schema2ProtoConfiguration configuration) {
		this.configuration = configuration;
		init();
	}

	public Map<String, ProtoFile> parse() throws SAXException, IOException {

		SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
		saxParserFactory.setNamespaceAware(true);

		XSOMParser parser = new XSOMParser(saxParserFactory);
		parser.setErrorHandler(this);

		parser.setAnnotationParser(new DomAnnotationParserFactory());
		parser.parse(configuration.xsdFile);

		processSchemaSet(parser.getResult());

		return packageToProtoFileMap;
	}

	private void addType(String namespace, Type type) {
		ProtoFile file = getProtoFileForNamespace(namespace);
		file.types().add(type);

		// Type auto generated collision avoidance
		String typeName = null;

		if (type instanceof MessageType) {
			MessageType mt = (MessageType) type;
			typeName = mt.getName();
		} else if (type instanceof EnumType) {
			typeName = ((EnumType) type).name();
		}
	}

	private ProtoFile getProtoFileForNamespace(String namespace) {
		String packageName = NamespaceHelper.xmlNamespaceToProtoPackage(namespace, configuration.forceProtoPackage);
		if (StringUtils.trimToNull(packageName) == null) {
			packageName = DEFAULT_PROTO_PACKAGE;
		}

		if (configuration.defaultProtoPackage != null) {
			packageName = configuration.defaultProtoPackage;
		}

		ProtoFile file = packageToProtoFileMap.get(packageName);
		if (file == null) {
			file = new ProtoFile(Syntax.PROTO_3, packageName);
			packageToProtoFileMap.put(packageName, file);
		}
		return file;
	}

	private Type getType(String namespace, String typeName) {
		ProtoFile protoFileForNamespace = getProtoFileForNamespace(namespace);
		for (Type t : protoFileForNamespace.types()) {
			if (t instanceof MessageType) {
				if (((MessageType) t).getName().equals(typeName)) {
					return t;
				}
			} else if (t instanceof EnumType) {
				if (((EnumType) t).name().equals(typeName)) {
					return t;
				}
			}
		}

		return null;
	}

	private void processSchemaSet(XSSchemaSet schemaSet) throws ConversionException {

		Iterator<XSSchema> schemas = schemaSet.iterateSchema();
		while (schemas.hasNext()) {
			XSSchema schema = schemas.next();
			if (!schema.getTargetNamespace().endsWith("/XMLSchema")) {
				final Iterator<XSSimpleType> simpleTypes = schema.iterateSimpleTypes();
				while (simpleTypes.hasNext()) {
					processSimpleType(simpleTypes.next(), null);
				}
				final Iterator<XSComplexType> complexTypes = schema.iterateComplexTypes();
				while (complexTypes.hasNext()) {
					processComplexType(complexTypes.next(), null, schemaSet, null, null);
				}
				final Iterator<XSElementDecl> elementDeclarations = schema.iterateElementDecls();
				while (elementDeclarations.hasNext()) {
					XSElementDecl rootElement = elementDeclarations.next();
					if (rootElement.getType().isLocal()) {
						processElement(rootElement, schemaSet);
					} else {
						LOGGER.info("Skipping global element " + rootElement.getName() + " declaration with global type " + rootElement.getType().getName());
					}
				}
			}
		}
	}

	private String processElement(XSElementDecl el, XSSchemaSet schemaSet) throws ConversionException {
		XSComplexType cType;
		XSSimpleType xs;

		if (el.getType() instanceof XSComplexType && el.getType() != schemaSet.getAnyType()) {
			cType = (XSComplexType) el.getType();
			MessageType t = processComplexType(cType, el.getName(), schemaSet, null, null);
			return t.getName();
		} else if (el.getType() instanceof XSSimpleType && el.getType() != schemaSet.getAnySimpleType()) {
			xs = el.getType().asSimpleType();
			return processSimpleType(xs, el.getName());
		} else {
			LOGGER.info("Unhandled element " + el + " at " + el.getLocator().getSystemId() + " at line/col " + el.getLocator().getLineNumber() + "/"
					+ el.getLocator().getColumnNumber());

			return null;
		}
	}

	/**
	 * @param xs
	 * @param elementName
	 */
	private String processSimpleType(XSSimpleType xs, String elementName) {

		nestlevel++;

		LOGGER.debug(StringUtils.leftPad(" ", nestlevel) + "SimpleType " + xs);

		String typeName = xs.getName();

		if (typeName == null) {
			typeName = elementName + GENERATED_NAME_SUFFIX_UNIQUENESS;
		}

		if (xs.isRestriction() && xs.getFacet("enumeration") != null) {
			createEnum(typeName, xs.asRestriction(), null);
		} else {
			// This is just a restriction on a basic type, find parent and messages
			// it to the type
			String baseTypeName = typeName;
			while (xs != null && !basicTypes.contains(baseTypeName)) {
				xs = xs.getBaseType().asSimpleType();
				if (xs != null) {
					baseTypeName = xs.getName();
				}
			}
			simpleTypes.put(typeName, xs != null ? xs.getName() : "string");
			String doc = resolveDocumentationAnnotation(xs);
			addDocumentation(typeName, doc);
		}

		nestlevel--;
		return typeName;
	}

	private void addDocumentation(String typeName, String doc) {
		if (doc != null) {
			documentation.put(typeName, doc);
		}
	}

	private void addField(MessageType message, Field newField) {
		addField(message, null, newField);
	}

	private void addField(MessageType message, OneOf oneOf, Field newField) {
		// Remove old fields of same type (element or attribute)
		Field existingField = null;
		for (Field f : message.fieldsAndOneOfFields()) {
			if (newField.name().equals(f.name())) {
				existingField = f;
				break;
			}
		}
		if (existingField != null) {
			// Override should happen
			if (existingField.isFromAttribute() && !newField.isFromAttribute()) {
				existingField.updateName("attr_" + existingField.name());
			} else if (!existingField.isFromAttribute() && newField.isFromAttribute()) {
				newField.updateName("attr_" + newField.name());
			} else {
				message.removeDeclaredField(existingField);
			}
		}

		if (oneOf != null) {
			oneOf.addField(newField);
		} else {
			message.addField(newField);
		}
	}

	private void navigateSubTypes(XSParticle parentParticle, MessageType messageType, Set<Object> processedXmlObjects, XSSchemaSet schemaSet)
			throws ConversionException {

		XSTerm currTerm = parentParticle.getTerm();
		if (currTerm.isElementDecl()) {
			XSElementDecl currElementDecl = currTerm.asElementDecl();

			if (!processedXmlObjects.contains(currElementDecl)) {
				processedXmlObjects.add(currElementDecl);

				XSType type = currElementDecl.getType();

				if (type.isSimpleType()) {
					String doc = resolveDocumentationAnnotation(currElementDecl);

					if (type.asSimpleType().isRestriction() && type.asSimpleType().getFacet("enumeration") != null) {

						String enumName = createEnum(currElementDecl.getName(), type.asSimpleType().asRestriction(), type.isGlobal() ? null : messageType);

						boolean extension = false;
						Options options = getFieldOptions(parentParticle);
						int tag = messageType.getNextFieldNum();
						Label label = getRange(parentParticle) ? Label.REPEATED : null;
						Location location = getLocation(currElementDecl);

						Field field = new Field(NamespaceHelper.xmlNamespaceToProtoFieldPackagename(type.getTargetNamespace(), configuration.forceProtoPackage),
								location, label, currElementDecl.getName(), doc, tag, null, enumName, options, extension, true);
						addField(messageType, field);

						XSRestrictionSimpleType restriction = type.asSimpleType().asRestriction();
						// checkType(restriction.getBaseType());
						// TODO ENUM

					} else {
						//
						Options options = getFieldOptions(parentParticle);
						int tag = messageType.getNextFieldNum();
						Label label = getRange(parentParticle) ? Label.REPEATED : null;
						Location location = getLocation(currElementDecl);

						String typeName = findFieldType(type);

						String packageName = NamespaceHelper.xmlNamespaceToProtoFieldPackagename(type.getTargetNamespace(), configuration.forceProtoPackage);

						Field field = new Field(basicTypes.contains(typeName) ? null : packageName, location, label, currElementDecl.getName(), doc, tag, null,
								typeName, options, false, true); // TODO add
						// restriction
						// as
						// validation
						// parameters
						addField(messageType, field);
					}

				} else {
					// Complex type

					if (type.isGlobal()) {

						// COMPLEX TYPE
						Set<? extends XSElementDecl> substitutables = currElementDecl.getSubstitutables();
						String doc = resolveDocumentationAnnotation(currElementDecl);

						Options options = getFieldOptions(parentParticle);

						if (substitutables.size() <= 1) {

							Label label = getRange(parentParticle) ? Label.REPEATED : null;
							Location location = getLocation(currElementDecl);
							int tag = messageType.getNextFieldNum();
							Field field = new Field(
									NamespaceHelper.xmlNamespaceToProtoFieldPackagename(type.getTargetNamespace(), configuration.forceProtoPackage), location,
									label, currElementDecl.getName(), doc, tag, null, type.getName(), options, false, true);
							addField(messageType, field);
						} else {
							List<Field> fields = new ArrayList<>();
							OneOf oneOf = new OneOf(currElementDecl.getType().getName(), doc, fields);
							messageType.oneOfs().add(oneOf);
							for (XSElementDecl substitutable : substitutables) {
								if (substitutable.isAbstract()) {

								} else {
									String substDoc = resolveDocumentationAnnotation(substitutable);

									String typeName = substitutable.getType().getName();
									if (typeName == null) {
										typeName = processElement(substitutable, schemaSet);
									}

									int tag = messageType.getNextFieldNum();
									Location location = getLocation(substitutable);
									Field field = new Field(
											NamespaceHelper.xmlNamespaceToProtoFieldPackagename(substitutable.getType().getTargetNamespace(),
													configuration.forceProtoPackage),
											location, null, substitutable.getName(), substDoc, tag, null, typeName, options, false, true);
									addField(messageType, oneOf, field); // Repeated oneOf not allowed
								}
							}
						}
					} else {
						// Local

						MessageType referencedMessageType = processComplexType(type.asComplexType(), currElementDecl.getName(), schemaSet, null, null);

						String fieldDoc = resolveDocumentationAnnotation(currElementDecl);

						Options options = getFieldOptions(parentParticle);
						int tag = messageType.getNextFieldNum();
						Label label = getRange(parentParticle) ? Label.REPEATED : null;
						Location fieldLocation = getLocation(currElementDecl);

						Field field = new Field(
								NamespaceHelper.xmlNamespaceToProtoFieldPackagename(type.asComplexType().getTargetNamespace(), configuration.forceProtoPackage),
								fieldLocation, label, currElementDecl.getName(), fieldDoc, tag, null, referencedMessageType.getName(), options, false, true);
						addField(messageType, field);

					}
				}
			}
		} else {
			XSModelGroupDecl modelGroupDecl = null;
			XSModelGroup modelGroup = null;
			modelGroup = getModelGroup(modelGroupDecl, currTerm);

			if (modelGroup != null) {

				groupProcessing(modelGroup, parentParticle, messageType, processedXmlObjects, schemaSet);
			}

		}
	}

	@NotNull
	private Options getFieldOptions(XSParticle parentParticle) {
		List<OptionElement> optionElements = new ArrayList<OptionElement>(ruleFactory.getValidationRule(parentParticle));
		return new Options(Options.FIELD_OPTIONS, optionElements);
	}

	@NotNull
	private Options getFieldOptions(XSAttributeDecl attributeDecl) {
		List<OptionElement> optionElements = new ArrayList<OptionElement>();

		// First see if there are rules associated with attribute declaration
		List<OptionElement> validationRule = ruleFactory.getValidationRule(attributeDecl);
		if (validationRule.size() > 0) {
			optionElements.addAll(validationRule);
		} else {
			// Check attribute TYPE rules
			List<OptionElement> typeRule = ruleFactory.getValidationRule(attributeDecl.getType());
			optionElements.addAll(typeRule);
		}
		return new Options(Options.FIELD_OPTIONS, optionElements);
	}

	@NotNull
	private Options getFieldOptions(XSSimpleType attributeDecl) {
		List<OptionElement> optionElements = new ArrayList<OptionElement>(ruleFactory.getValidationRule(attributeDecl));
		return new Options(Options.FIELD_OPTIONS, optionElements);
	}

	public String findFieldType(XSType type) {
		String typeName = type.getName();
		if (typeName == null) {

			if (type.asSimpleType().isRestriction()) {

				try {
					return findFieldType(type.asSimpleType().asRestriction().getBaseType());

				} catch (ClassCastException e) {
					LOGGER.warn(
							"Error getting base type for restriction " + type + ". Appears to be a bug in xsom. Fallback to string field type (best guess)");
					return "string";
				}

				// findFieldType((findBaseType(restriction));
			} else if (type.asSimpleType().isPrimitive()) {
				typeName = type.asSimpleType().getName();
			} else if (type.asSimpleType().isList()) {
				XSListSimpleType asList = type.asSimpleType().asList();
				XSSimpleType itemType = asList.getItemType();
				typeName = itemType.getName();
			} else if (type.asSimpleType().isUnion()) {
				typeName = "string"; // Union always resolves to string
			} else {
				typeName = type.asSimpleType().getBaseType().getName();
			}

		} else {
			if (!basicTypes.contains(typeName)) {
				typeName = type.asSimpleType().getBaseType().getName();
			}
		}

		if ((typeName != null && !basicTypes.contains(typeName) || typeName == null) && type.isSimpleType() && type.asSimpleType().isRestriction()) {
			XSType restrictionBase = type.asSimpleType().asRestriction().getBaseType();
			return findFieldType(restrictionBase);
		}
		return typeName;
	}

	private boolean getRange(XSParticle part) {
		int max = 1;
		int min = 1;

		if (part.getMinOccurs() != null) {
			min = part.getMinOccurs().intValue();
		}

		if (part.getMaxOccurs() != null) {
			max = part.getMaxOccurs().intValue();
		}

		return max > 1 || max == -1 || min > 1;
	}

	/**
	 * @param complexType
	 * @param elementName
	 * @param schemaSet
	 * @throws ConversionException
	 */
	private MessageType processComplexType(XSComplexType complexType, String elementName, XSSchemaSet schemaSet, MessageType messageType,
			Set<Object> processedXmlObjects) throws ConversionException {

		nestlevel++;

		LOGGER.debug(StringUtils.leftPad(" ", nestlevel) + "ComplexType " + complexType + ", proto " + messageType);

		boolean isBaseLevel = messageType == null;

		String typeName = null;
		if (messageType != null) {
			typeName = messageType.getName();
		}

		if (messageType == null) {
			if (configuration.skipEmptyTypeInheritance) {

				while (complexType.getContentType().asParticle() == null && complexType.getAttributeUses().size() == 0
						&& complexType.getBaseType().isComplexType()) {
					// Empty complex type
					complexType = complexType.getBaseType().asComplexType();
				}
			}
			typeName = complexType.getName();
			String nameSpace = complexType.getTargetNamespace();

			if (complexType.getScope() != null) {
				elementName = complexType.getScope().getName();
			}

			if (typeName == null) {
				typeName = elementName + GENERATED_NAME_SUFFIX_UNIQUENESS;
			}
			messageType = (MessageType) getType(nameSpace, typeName);

			if (messageType == null && !basicTypes.contains(typeName)) {

				String doc = resolveDocumentationAnnotation(complexType);

				List<OptionElement> messageOptions = new ArrayList<>();
				Options options = new Options(Options.MESSAGE_OPTIONS, messageOptions);
				Location location = getLocation(complexType);
				List<Field> fields = new ArrayList<>();
				List<Field> extensions = new ArrayList<>();
				List<OneOf> oneofs = new ArrayList<>();
				List<Type> nestedTypes = new ArrayList<>();
				List<Extensions> extendsions = new ArrayList<>();
				List<Reserved> reserved = new ArrayList<>();
				// Add message type to file
				messageType = new MessageType(ProtoType.get(typeName), location, doc, typeName, fields, extensions, oneofs, nestedTypes, extendsions, reserved,
						options);

				addType(nameSpace, messageType);

				processedXmlObjects = new HashSet<>();

				elementDeclarationsPerMessageType.put(messageType, processedXmlObjects);

			} else {
				LOGGER.debug(StringUtils.leftPad(" ", nestlevel) + "Already processed ComplexType " + typeName + ", ignored");
				nestlevel--;
				return messageType;
			}
		}

		XSType parent = complexType.getBaseType();

		if (configuration.inheritanceToComposition && complexType.getContentType().asParticle() != null) {

			List<MessageType> parentTypes = new ArrayList<>();

			while (parent != schemaSet.getAnyType() && parent.isComplexType()) {

				// Ensure no duplicate element parsing
				MessageType parentType = processComplexType(parent.asComplexType(), elementName, schemaSet, null, null);
				processedXmlObjects.addAll(elementDeclarationsPerMessageType.get(parentType));

				parentTypes.add(parentType);
				parent = parent.getBaseType();
			}

			if (!isAbstract(complexType)) {
				Collections.reverse(parentTypes);
				for (MessageType parentMessageType : parentTypes) {
					String fieldDoc = parentMessageType.documentation();
					boolean extension = false;
					List<OptionElement> optionElements = new ArrayList<OptionElement>();
					Options fieldOptions = new Options(Options.FIELD_OPTIONS, optionElements);
					int tag = messageType.getNextFieldNum();
					Label label = null;
					Location fieldLocation = getLocation(complexType);

					Field field = new Field(findPackageNameForType(parentMessageType), fieldLocation, label, "_" + parentMessageType.getName(), fieldDoc, tag,
							null, parentMessageType.getName(), fieldOptions, extension, true);

					addField(messageType, field);
				}
				if (parentTypes.size() > 0) {
					messageType.advanceFieldNum();
				}
			}

		} else {
			if (parent != schemaSet.getAnyType() && parent.isComplexType()) {
				processComplexType(parent.asComplexType(), elementName, schemaSet, messageType, processedXmlObjects);
			}
		}

		if (complexType.getAttributeUses() != null) {
			processAttributes(complexType, messageType, processedXmlObjects);
		}

		if (complexType.getContentType().asParticle() != null) {
			XSParticle particle = complexType.getContentType().asParticle();

			XSTerm term = particle.getTerm();
			XSModelGroupDecl modelGroupDecl = null;
			XSModelGroup modelGroup = null;
			modelGroup = getModelGroup(modelGroupDecl, term);

			if (modelGroup != null) {
				groupProcessing(modelGroup, particle, messageType, processedXmlObjects, schemaSet);
			}

		} else if (complexType.getContentType().asSimpleType() != null) {
			XSSimpleType xsSimpleType = complexType.getContentType().asSimpleType();

			if (isBaseLevel) { // Only add simpleContent from concrete type?
				/*
				 * if(!processedXmlObjects.contains(xsSimpleType)) processedXmlObjects.add(xsSimpleType);
				 */

				boolean isList = xsSimpleType.isList();
				if (isList) {
					xsSimpleType = xsSimpleType.asList().getItemType();
				}

				String name = null;
				if (xsSimpleType.isUnion()) {
					name = "string";
				} else {
					name = xsSimpleType.getName();
				}

				if (name == null) {
					// Add simple field
					boolean extension = false;
					Options options = getFieldOptions(xsSimpleType);
					int tag = messageType.getNextFieldNum();

					Location fieldLocation = getLocation(xsSimpleType);

					String simpleTypeName = findFieldType(xsSimpleType);

					String packageName = NamespaceHelper.xmlNamespaceToProtoFieldPackagename(xsSimpleType.getTargetNamespace(),
							configuration.forceProtoPackage);
					Label label = isList ? Label.REPEATED : null;
					Field field = new Field(basicTypes.contains(simpleTypeName) ? null : packageName, fieldLocation, label, "value",
							"SimpleContent value of element", tag, null, simpleTypeName, options, extension, true);
					addField(messageType, field);

				} else if (basicTypes.contains(name)) {

					// Add simple field
					boolean extension = false;
					Options options = getFieldOptions(xsSimpleType);
					int tag = messageType.getNextFieldNum();
					Label label = isList ? Label.REPEATED : null;
					Location fieldLocation = getLocation(xsSimpleType);

					Field field = new Field(null, fieldLocation, label, "value", "SimpleContent value of element", tag, null, name, options, extension, true);
					addField(messageType, field);

				} else {
					XSSimpleType primitiveType = xsSimpleType.getPrimitiveType();
					if (primitiveType != null) {

						// Add simple field
						boolean extension = false;
						List<OptionElement> optionElements = new ArrayList<>();
						Options fieldOptions = new Options(Options.FIELD_OPTIONS, optionElements);
						int tag = messageType.getNextFieldNum();
						Label label = isList ? Label.REPEATED : null;

						Location fieldLocation = getLocation(xsSimpleType);

						Field field = new Field(null, fieldLocation, label, "value", "SimpleContent value of element", tag, null, primitiveType.getName(),
								fieldOptions, extension, true);
						addField(messageType, field);

					}
				}
			}
		}

		nestlevel--;
		return messageType;

	}

	private boolean isAbstract(XSComplexType complexType) {
		if (complexType.isAbstract()) {
			return true;
		} else {
			if (!complexType.isGlobal()) {
				return false;
			} else if (complexType.getElementDecls().size() > 0) {
				long numAbstractElementDecls = complexType.getElementDecls().stream().map(e -> e.isAbstract()).count();
				return complexType.getElementDecls().size() == numAbstractElementDecls;
			} else {
				return true;
			}

		}
	}

	private String findPackageNameForType(MessageType parentMessageType) {
		// Slow and dodgy
		for (Map.Entry<String, ProtoFile> packageAndProtoFile : packageToProtoFileMap.entrySet()) {
			ProtoFile file = packageAndProtoFile.getValue();
			for (Type t : file.types()) {
				if (t == parentMessageType) {
					return packageAndProtoFile.getKey();
				}
			}

		}
		return null;
	}

	private Location getLocation(XSComponent t) {
		Locator l = t.getLocator();
		return new Location("", l.getSystemId(), l.getLineNumber(), l.getColumnNumber());
	}

	private void processAttributes(XSAttContainer xsAttContainer, MessageType messageType, Set<Object> processedXmlObjects) {
		Iterator<? extends XSAttributeUse> iterator = xsAttContainer.iterateDeclaredAttributeUses();
		while (iterator.hasNext()) {
			XSAttributeUse attr = iterator.next();
			processAttribute(messageType, processedXmlObjects, attr);
		}

		Iterator<? extends XSAttGroupDecl> iterateAttGroups = xsAttContainer.iterateAttGroups();
		while (iterateAttGroups.hasNext()) {
			// Recursive
			processAttributes(iterateAttGroups.next(), messageType, processedXmlObjects);
		}

	}

	private void processAttribute(MessageType messageType, Set<Object> processedXmlObjects, XSAttributeUse attr) {
		if (!processedXmlObjects.contains(attr)) {
			processedXmlObjects.add(attr);

			XSAttributeDecl decl = attr.getDecl();

			if (decl.getType().getPrimitiveType() != null) {
				String fieldName = decl.getName();
				String doc = resolveDocumentationAnnotation(decl);

				if (decl.getType().isRestriction() && decl.getType().getFacet("enumeration") != null) {

					String enumName = createEnum(fieldName, decl.getType().asRestriction(), decl.isLocal() ? messageType : null);

					boolean extension = false;
					List<OptionElement> optionElements = new ArrayList<OptionElement>();
					Options fieldOptions = new Options(Options.FIELD_OPTIONS, optionElements);
					int tag = messageType.getNextFieldNum();
					// Label label = attr. ? Label.REPEATED : null;
					Location fieldLocation = getLocation(decl);

					Field field = new Field(
							NamespaceHelper.xmlNamespaceToProtoFieldPackagename(decl.getType().getTargetNamespace(), configuration.forceProtoPackage),
							fieldLocation, null, fieldName, doc, tag, null, enumName, fieldOptions, extension, false);
					field.setFromAttribute(true);
					addField(messageType, field);

				} else {

					Options options = getFieldOptions(decl);
					int tag = messageType.getNextFieldNum();
					// Label label = attr. ? Label.REPEATED : null;
					Location fieldLocation = getLocation(decl);

					String typeName = findFieldType(decl.getType());

					String packageName = NamespaceHelper.xmlNamespaceToProtoFieldPackagename(decl.getType().getTargetNamespace(),
							configuration.forceProtoPackage);

					Field field = new Field(basicTypes.contains(typeName) ? null : packageName, fieldLocation, null, fieldName, doc, tag, null, typeName,
							options, false, false);
					field.setFromAttribute(true);
					addField(messageType, field);

				}
			}
		}
	}

	private XSModelGroup getModelGroup(XSModelGroupDecl modelGroupDecl, XSTerm term) {
		if ((modelGroupDecl = term.asModelGroupDecl()) != null) {
			return modelGroupDecl.getModelGroup();
		} else if (term.isModelGroup()) {
			return term.asModelGroup();
		} else {
			return null;
		}
	}

	private String createWrapperName(MessageType enclosingType, XSModelGroup.Compositor compositor) {

		final String wrapperPrefix;
		if (XSModelGroup.SEQUENCE.equals(compositor)) {
			wrapperPrefix = "SequenceWrapper";
		} else if (XSModelGroup.CHOICE.equals(compositor)) {
			wrapperPrefix = "ChoiceWrapper";
		} else {
			throw new ConversionException("Cannot wrap message with compositor?" + compositor);
		}

		long numExistingWrappers = enclosingType.nestedTypes()
				.stream()
				.filter(e -> e instanceof MessageType)
				.map(e -> (MessageType) e)
				.filter(e -> e.getName().startsWith(wrapperPrefix))
				.count();
		if (numExistingWrappers == 0) {
			return wrapperPrefix;
		} else {
			return wrapperPrefix + (numExistingWrappers + 1);
		}

	}

	private void groupProcessing(XSModelGroup modelGroup, XSParticle particle, MessageType messageType, Set<Object> processedXmlObjects, XSSchemaSet schemaSet)
			throws ConversionException {
		XSModelGroup.Compositor compositor = modelGroup.getCompositor();

		XSParticle[] children = modelGroup.getChildren();

		if (compositor.equals(XSModelGroup.ALL)) {
			processGroupAsSequence(particle, messageType, processedXmlObjects, schemaSet, children);
		} else if (compositor.equals(XSModelGroup.SEQUENCE)) {
			boolean repeated = getRange(particle);
			if (repeated) {

				String typeName = createWrapperName(messageType, XSModelGroup.SEQUENCE);
				createWrapperAndContinueProcessing(modelGroup, particle, messageType, processedXmlObjects, schemaSet, children, typeName);

			} else {
				processGroupAsSequence(particle, messageType, processedXmlObjects, schemaSet, children);
			}

		} else if (compositor.equals(XSModelGroup.CHOICE)) {
			// Check if choice is unbounded, if so create repeated wrapper class and then continue
			boolean repeated = getRange(particle);
			if (repeated) {

				String typeName = createWrapperName(messageType, XSModelGroup.CHOICE);
				createWrapperAndContinueProcessing(modelGroup, particle, messageType, processedXmlObjects, schemaSet, children, typeName);

			} else {
				processGroupAsSequence(particle, messageType, processedXmlObjects, schemaSet, children);
			}
		}
		messageType.advanceFieldNum();

	}

	private void createWrapperAndContinueProcessing(XSModelGroup modelGroup, XSParticle particle, MessageType messageType, Set<Object> processedXmlObjects,
			XSSchemaSet schemaSet, XSParticle[] children, String typeName) {
		String fieldName = typeName; // Identical for now
		// Create new message type enclosed in existing
		// Add repeated field
		String doc = resolveDocumentationAnnotation(modelGroup);

		List<OptionElement> messageOptions = new ArrayList<>();
		Options options = new Options(Options.MESSAGE_OPTIONS, messageOptions);
		Location location = getLocation(modelGroup);
		List<Field> fields = new ArrayList<>();
		List<Field> extensions = new ArrayList<>();
		List<OneOf> oneofs = new ArrayList<>();
		List<Type> nestedTypes = new ArrayList<>();
		List<Extensions> extendsions = new ArrayList<>();
		List<Reserved> reserved = new ArrayList<>();
		// Add message type to file
		MessageType wrapperType = new MessageType(ProtoType.get(typeName), location, doc, typeName, fields, extensions, oneofs, nestedTypes, extendsions,
				reserved, options);
		wrapperType.setWrapperMessageType(true);
		messageType.nestedTypes().add(wrapperType);

		Options fieldOptions = getFieldOptions(particle);
		int tag = messageType.getNextFieldNum();

		Field field = new Field(null, location, Label.REPEATED, fieldName, doc, tag, null, typeName, fieldOptions, false, false);

		messageType.addField(field);

		processGroupAsSequence(particle, wrapperType, processedXmlObjects, schemaSet, children);
	}

	private void processGroupAsSequence(XSParticle particle, MessageType messageType, Set<Object> processedXmlObjects, XSSchemaSet schemaSet,
			XSParticle[] children) {
		for (int i = 0; i < children.length; i++) {
			XSTerm currTerm = children[i].getTerm();
			if (currTerm.isModelGroup()) {
				groupProcessing(currTerm.asModelGroup(), particle, messageType, processedXmlObjects, schemaSet);
			} else {
				// Create the new complex type for root types
				navigateSubTypes(children[i], messageType, processedXmlObjects, schemaSet);
			}
		}
	}

	private String resolveDocumentationAnnotation(XSComponent xsComponent) {
		String doc = "";
		if (xsComponent.getAnnotation() != null && xsComponent.getAnnotation().getAnnotation() != null) {
			if (xsComponent.getAnnotation().getAnnotation() instanceof Node) {
				Node annotationEl = (Node) xsComponent.getAnnotation().getAnnotation();
				NodeList annotations = annotationEl.getChildNodes();

				for (int i = 0; i < annotations.getLength(); i++) {
					Node annotation = annotations.item(i);
					if ("documentation".equals(annotation.getLocalName())) {

						NodeList childNodes = annotation.getChildNodes();
						for (int j = 0; j < childNodes.getLength(); j++) {
							if (childNodes.item(j) != null && childNodes.item(j) instanceof Text) {
								doc = childNodes.item(j).getNodeValue();
							}
						}
					}
				}
			}
		}

		String[] lines = doc.split("\n");
		StringBuilder b = new StringBuilder();
		for (String line : lines) {
			b.append(StringUtils.trimToEmpty(line));
			b.append(" ");
		}

		if (configuration.includeSourceLocationInDoc) {
			if (xsComponent != null && xsComponent.getLocator() != null) {
				Location loc = getLocation(xsComponent);
				b.append(" [");
				b.append(loc.toString());
				b.append("]");
			}
		}
		return StringUtils.trimToEmpty(b.toString());
	}

	private String createEnum(String elementName, XSRestrictionSimpleType type, MessageType enclosingType) {
		Iterator<? extends XSFacet> it;

		String typeNameToUse = null;

		if (type.getName() != null) {
			typeNameToUse = type.getName();
			enclosingType = null;
		} else {

			String baseTypeName = type.getBaseType().getName();
			if (baseTypeName != null && !basicTypes.contains(baseTypeName)) {
				typeNameToUse = baseTypeName;
			} else if (enclosingType != null) {
				typeNameToUse = elementName + TYPE_SUFFIX;
			} else {
				typeNameToUse = elementName + GENERATED_NAME_SUFFIX_UNIQUENESS;
			}
		}

		Type protoType = getType(type.getTargetNamespace(), typeNameToUse);
		if (protoType == null) {

			type = type.asRestriction();

			Location location = getLocation(type);

			List<EnumConstant> constants = new ArrayList<EnumConstant>();
			it = type.getDeclaredFacets().iterator();

			int counter = 1;
			Set<String> addedValues = new HashSet<>();
			while (it.hasNext()) {
				List<OptionElement> optionElements = new ArrayList<>();
				XSFacet next = it.next();
				String doc = resolveDocumentationAnnotation(next);
				String enumValue = next.getValue().value;

				if (!addedValues.contains(enumValue)) {
					addedValues.add(enumValue);
					constants.add(new EnumConstant(location, enumValue, counter++, doc, new Options(Options.ENUM_VALUE_OPTIONS, optionElements)));
				}
			}

			List<OptionElement> enumOptionElements = new ArrayList<>();
			Options enumOptions = new Options(Options.ENUM_OPTIONS, enumOptionElements);

			String doc = resolveDocumentationAnnotation(type);

			ProtoType definedProtoType;
			if (enclosingType == null) {
				definedProtoType = ProtoType.get(typeNameToUse);
			} else {
				definedProtoType = ProtoType.get(enclosingType.getName(), typeNameToUse);
			}

			EnumType enumType = new EnumType(definedProtoType, location, doc, typeNameToUse, constants, enumOptions);

			if (enclosingType != null) {
				// if not already present
				boolean alreadyPresentAsNestedType = false;
				for (Type t : enclosingType.nestedTypes()) {
					if (t instanceof EnumType && ((EnumType) t).name().equals(typeNameToUse)) {
						alreadyPresentAsNestedType = true;
						break;
					}
				}
				if (!alreadyPresentAsNestedType) {
					enclosingType.nestedTypes().add(enumType);
				}
			} else {
				addType(type.getTargetNamespace(), enumType);
			}
		}
		return typeNameToUse;
	}

	@Override
	public void error(SAXParseException exception) throws SAXException {
		LOGGER.error(exception.getMessage() + " at " + exception.getSystemId());
		exception.printStackTrace();
	}

	@Override
	public void fatalError(SAXParseException exception) throws SAXException {
		LOGGER.error(exception.getMessage() + " at " + exception.getSystemId());
		exception.printStackTrace();
	}

	@Override
	public void warning(SAXParseException exception) throws SAXException {
		LOGGER.warn(exception.getMessage() + " at " + exception.getSystemId());
		exception.printStackTrace();
	}

}
