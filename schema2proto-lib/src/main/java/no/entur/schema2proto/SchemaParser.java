package no.entur.schema2proto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.ErrorHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.google.common.collect.ImmutableList;
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

	private static final String DEFAULT_PROTO_PACKAGE = "default";

	private static final Logger LOGGER = LoggerFactory.getLogger(SchemaParser.class);

	private Map<String, ProtoFile> packageToProtoFileMap = new HashMap<>();

	private Map<String, String> simpleTypes;
	private Map<String, String> documentation;
	private Set<String> basicTypes;

	private int nestlevel = 0;

	private Schema2ProtoConfiguration configuration;

	private void init() {
		simpleTypes = new HashMap<String, String>();
		documentation = new HashMap<String, String>();

		basicTypes = new TreeSet<String>();
		basicTypes.add("string");
		basicTypes.add("normalizedString");
		basicTypes.add("anyType");
		basicTypes.add("anyURI");
		basicTypes.add("anySimpleType");
		basicTypes.add("language");

		basicTypes.add("integer");
		basicTypes.add("positiveInteger");
		basicTypes.add("nonPositiveInteger");
		basicTypes.add("negativeInteger");
		basicTypes.add("nonNegativeInteger");

		basicTypes.add("unsignedLong");
		basicTypes.add("unsignedInt");
		basicTypes.add("unsignedShort");
		basicTypes.add("unsignedByte");

		basicTypes.add("base64Binary");
		basicTypes.add("hexBinary");
		basicTypes.add("boolean");
		basicTypes.add("date");
		basicTypes.add("dateTime");
		basicTypes.add("time");
		basicTypes.add("duration");
		basicTypes.add("decimal");
		basicTypes.add("float");
		basicTypes.add("double");
		basicTypes.add("byte");
		basicTypes.add("short");
		basicTypes.add("long");
		basicTypes.add("int");
		basicTypes.add("ID");
		basicTypes.add("IDREF");
		basicTypes.add("NMTOKEN");
		basicTypes.add("NMTOKENS");
		basicTypes.add("Name");

	}

	public SchemaParser(Schema2ProtoConfiguration configuration) {
		this.configuration = configuration;
		init();
	}

	public Map<String, ProtoFile> parse() throws Exception {

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
					processElement(elementDeclarations.next(), schemaSet);
				}
			}
		}
	}

	private void processElement(XSElementDecl el, XSSchemaSet schemaSet) throws ConversionException {
		XSComplexType cType;
		XSSimpleType xs;

		if (el.getType() instanceof XSComplexType && el.getType() != schemaSet.getAnyType()) {
			cType = (XSComplexType) el.getType();
			processComplexType(cType, el.getName(), schemaSet, null, null);
		} else if (el.getType() instanceof XSSimpleType && el.getType() != schemaSet.getAnySimpleType()) {
			xs = el.getType().asSimpleType();
			processSimpleType(xs, el.getName());
		} else {
			LOGGER.info("Unhandled element " + el + " at " + el.getLocator().getSystemId() + " at line/col " + el.getLocator().getLineNumber() + "/"
					+ el.getLocator().getColumnNumber());
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
			if (xs.getFacet("enumeration") != null) {
				typeName = elementName != null ? elementName + "Type" : generateAnonymousName();
			} else {
				// can't use elementName here as it might not be unique
				// (test-range.xsd)
				typeName = generateAnonymousName();
			}
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

	private void addField(MessageType message, Field field, boolean acceptDuplicate) {

		ImmutableList<Field> fields = message.fields();
		Field existingField = null;
		for (Field f : fields) {
			if (field.name().equals(f.name())) {
				existingField = f;
				break;
			}
		}

		if (existingField != null) {
			message.removeDeclaredField(existingField);
			if (acceptDuplicate) {
				field.setLabel(Label.REPEATED);
			}
		}

		message.addField(field);

	}

	private void navigateSubTypes(XSParticle parentParticle, MessageType messageType, Set<Object> processedXmlObjects, XSSchemaSet schemaSet,
			boolean isExtension) throws ConversionException {
		XSTerm currTerm = parentParticle.getTerm();

		if (currTerm.isElementDecl()) {
			XSElementDecl currElementDecl = currTerm.asElementDecl();

			if (!processedXmlObjects.contains(currElementDecl)) {
				processedXmlObjects.add(currElementDecl);

				XSType type = currElementDecl.getType();

				if (type != null && type.isComplexType() && type.getName() != null) {

					// COMPLEX TYPE

					String doc = resolveDocumentationAnnotation(currElementDecl);

					boolean extension = false;
					List<OptionElement> optionElements = new ArrayList<OptionElement>();
					Options options = new Options(Options.FIELD_OPTIONS, optionElements);
					int tag = messageType.getNextFieldNum();
					Label label = getRange(parentParticle) ? Label.REPEATED : null;
					Location location = getLocation(currElementDecl);

					Field field = new Field(NamespaceHelper.xmlNamespaceToProtoFieldPackagename(type.getTargetNamespace(), configuration.forceProtoPackage),
							location, label, currElementDecl.getName(), doc, tag, null, type.getName(), options, extension, true);
					addField(messageType, field, isExtension);

					// jolieType.addField(currElementDecl.getName(), type.getName(), getRange(parentParticle), null, null, xsdMapping);
					/*
					 * TypeDefinition jolieSimpleType = new TypeDefinitionLink(parsingContext, currElementDecl.getName(), getRange(parentParticle),
					 * complexTypes.get(type.getName() + TYPE_SUFFIX)); jolieType.putSubType(jolieSimpleType);
					 */

//				} else if (type != null && type.getName() != null) {// && simpleTypes.get(type.getName() + TYPE_SUFFIX) != null) {
//
//					// SIMPLE TYPE
//					/*
//					 * if ( simpleTypes.get( type.getName() + TYPE_SUFFIX ) == null ) { // create lazy type TypeDefinition jolieLazyType = new
//					 * TypeInlineDefinition( parsingContext, type.getName() + TYPE_SUFFIX, NativeType.ANY, Constants.RANGE_ONE_TO_ONE ); simpleTypes.put(
//					 * type.getName() + TYPE_SUFFIX, jolieLazyType ); }
//					 */
//
//					String doc = resolveDocumentationAnnotation(currElementDecl);
//
//					boolean extension = false;
//					List<OptionElement> optionElements = new ArrayList<OptionElement>();
//					Options options = new Options(Options.FIELD_OPTIONS, optionElements);
//					int tag = messageType.getNextFieldNum();
//					Label label = getRange(parentParticle) ? Label.REPEATED : null;
//					Location location = new Location("", "", 1, 1);
//
//					Field field = new Field("", location, label, currElementDecl.getName(), doc, tag, null, type.getName(), options, extension);
//					addField(messageType,field);
//
//					// jolieType.addField(currElementDecl.getName(), type.getName(), getRange(parentParticle), null, null, xsdMapping);
//
//					/*
//					 * TypeDefinition jolieSimpleType = new TypeDefinitionLink(parsingContext, currElementDecl.getName(), getRange(parentParticle),
//					 * simpleTypes.get(type.getName() + TYPE_SUFFIX)); jolieType.putSubType(jolieSimpleType);
//					 */

				} else {

					// checkDefaultAndFixed(currElementDecl);
					if (type.isSimpleType()) {
						// checkForNativeType(type, WARNING_2);

						// String typeName = processSimpleType(type.asSimpleType(), currElementDecl.getName());

						String doc = resolveDocumentationAnnotation(currElementDecl);

						if (type.asSimpleType().isRestriction() && type.asSimpleType().getFacet("enumeration") != null) {

							String enumName = createEnum(currElementDecl.getName(), type.asSimpleType().asRestriction(), messageType);
							// jolieType.addField(currElementDecl.getName(), enumName, false, null, null, xsdMapping);

							boolean extension = false;
							List<OptionElement> optionElements = new ArrayList<OptionElement>();
							Options options = new Options(Options.FIELD_OPTIONS, optionElements);
							int tag = messageType.getNextFieldNum();
							Label label = getRange(parentParticle) ? Label.REPEATED : null;
							Location location = getLocation(currElementDecl);

							Field field = new Field(
									NamespaceHelper.xmlNamespaceToProtoFieldPackagename(type.getTargetNamespace(), configuration.forceProtoPackage), location,
									label, currElementDecl.getName(), doc, tag, null, enumName, options, extension, true);
							addField(messageType, field, isExtension);

							XSRestrictionSimpleType restriction = type.asSimpleType().asRestriction();
							// checkType(restriction.getBaseType());
							// TODO ENUM
							// jolieType.putSubType(createSimpleType(restriction.getBaseType(), currElementDecl, Constants.RANGE_ONE_TO_ONE));
						} else {
							//
							boolean extension = false;
							List<OptionElement> optionElements = new ArrayList<OptionElement>();
							Options options = new Options(Options.FIELD_OPTIONS, optionElements);
							int tag = messageType.getNextFieldNum();
							Label label = getRange(parentParticle) ? Label.REPEATED : null;
							Location location = getLocation(currElementDecl);

							String typeName = findFieldType(type);

							String packageName = NamespaceHelper.xmlNamespaceToProtoFieldPackagename(type.getTargetNamespace(),
									configuration.forceProtoPackage);

							Field field = new Field(basicTypes.contains(typeName) ? null : packageName, location, label, currElementDecl.getName(), doc, tag,
									null, typeName, options, extension, true); // TODO add
							// restriction
							// as
							// validation
							// parameters
							addField(messageType, field, isExtension);
						}
					} else if (type.isComplexType()) {
						XSComplexType complexType = type.asComplexType();
						XSParticle particle;
						XSContentType contentType;
						contentType = complexType.getContentType();
						if ((particle = contentType.asParticle()) == null) {
							// jolieType.putSubType( createAnyOrUndefined( currElementDecl.getName(), complexType ) );
						}
						if (contentType.asSimpleType() != null) {
							// checkStrictModeForSimpleType(contentType);
						} else if ((particle = contentType.asParticle()) != null) {
							XSTerm term = particle.getTerm();
							XSModelGroupDecl modelGroupDecl = null;
							XSModelGroup modelGroup = null;
							modelGroup = getModelGroup(modelGroupDecl, term);
							if (modelGroup != null) {
								// ProtobufMessage jolieComplexType = new ProtobufMessage(currElementDecl.getName() + "Type", type.getTargetNamespace());
								// messages.put(jolieComplexType.getMessageName(), jolieComplexType);
								String complexTypeName = processComplexType(complexType, currElementDecl.getName(), schemaSet, null, null);

								/*
								 * String typeDoc = resolveDocumentationAnnotation(term);
								 * 
								 * List<OptionElement> messageOptions = new ArrayList<>(); Options options = new Options(Options.MESSAGE_OPTIONS,
								 * messageOptions); Location location = new Location("", "", 1, 1); List<Field> fields = new ArrayList<>(); List<Field>
								 * extensions = new ArrayList<>(); List<OneOf> oneofs = new ArrayList<>(); List<Type> nestedTypes = new ArrayList<>();
								 * List<Extensions> extendsions = new ArrayList<>(); List<Reserved> reserved = new ArrayList<>(); // Add message type to file
								 * MessageType nestedMessageType = new MessageType(ProtoType.BOOL, location, typeDoc, currElementDecl.getName() + "Type",
								 * fields, extensions, oneofs, nestedTypes, extendsions, reserved, options); messages.put(nestedMessageType.getName(),
								 * nestedMessageType);
								 * 
								 * addType(type.getTargetNamespace(), nestedMessageType);
								 * 
								 * // TypeInlineDefinition jolieComplexType = createComplexType(complexType, currElementDecl.getName(), particle);
								 * groupProcessing(modelGroup, particle, nestedMessageType, new HashSet<>()); // jolieType.putSubType(jolieComplexType); //
								 * jolieType.addField(currElementDecl.getName(), jolieComplexType.getMessageName(), getRange(parentParticle), null, null, //
								 * xsdMapping);
								 */
								String fieldDoc = resolveDocumentationAnnotation(currElementDecl);
								boolean extension = false;
								List<OptionElement> optionElements = new ArrayList<OptionElement>();
								Options fieldOptions = new Options(Options.FIELD_OPTIONS, optionElements);
								int tag = messageType.getNextFieldNum();
								Label label = getRange(parentParticle) ? Label.REPEATED : null;
								Location fieldLocation = getLocation(currElementDecl);

								Field field = new Field(
										NamespaceHelper.xmlNamespaceToProtoFieldPackagename(complexType.getTargetNamespace(), configuration.forceProtoPackage),
										fieldLocation, label, currElementDecl.getName(), fieldDoc, tag, null, complexTypeName, fieldOptions, extension, true);
								addField(messageType, field, isExtension);

							}
						}
					}
				}
			}
		} else {
			XSModelGroupDecl modelGroupDecl = null;
			XSModelGroup modelGroup = null;
			modelGroup = getModelGroup(modelGroupDecl, currTerm);

			if (modelGroup != null) {

				groupProcessing(modelGroup, parentParticle, messageType, processedXmlObjects, schemaSet, isExtension);
			}

		}
	}

	private String findFieldType(XSType type) {
		String typeName = type.getName();
		if (typeName == null) {

			if (type.asSimpleType().isPrimitive()) {
				typeName = type.asSimpleType().getName();
			} else if (type.asSimpleType().isList()) {
				XSListSimpleType asList = type.asSimpleType().asList();
				XSSimpleType itemType = asList.getItemType();
				typeName = itemType.getName();
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

		if (part.getMaxOccurs() != null) {
			max = part.getMaxOccurs().intValue();
		}

		return max > 1 || max == -1;
	}

	/**
	 * @param complexType
	 * @param elementName
	 * @param schemaSet
	 * @throws ConversionException
	 */
	private String processComplexType(XSComplexType complexType, String elementName, XSSchemaSet schemaSet, MessageType messageType,
			Set<Object> processedXmlObjects) throws ConversionException {

		nestlevel++;

		LOGGER.debug(StringUtils.leftPad(" ", nestlevel) + "ComplexType " + complexType + ", proto " + messageType);

		if (messageType == null && complexType.isAbstract()) {
			LOGGER.debug(StringUtils.leftPad(" ", nestlevel) + "Abstract ComplexType " + complexType + ", ignored");
			nestlevel--;
			return null; // Do not create messages from abstract types
		}

		boolean isBaseLevel = messageType == null;

		String typeName = null;
		if (messageType != null) {
			typeName = messageType.getName();
		}

		if (messageType == null) {

			typeName = complexType.getName();
			String nameSpace = complexType.getTargetNamespace();

			if (complexType.getScope() != null) {
				elementName = complexType.getScope().getName();
			}

			if (typeName == null) {
				typeName = elementName != null ? elementName + "Type" : generateAnonymousName();
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
				messageType = new MessageType(ProtoType.get(typeName), location, doc, typeName, fields, extensions, oneofs,
                        nestedTypes, extendsions, reserved, options);

				addType(nameSpace, messageType);

				processedXmlObjects = new HashSet<>();
			} else {
				LOGGER.debug(StringUtils.leftPad(" ", nestlevel) + "Already processed ComplexType " + typeName + ", ignored");
				nestlevel--;
				return typeName;
			}
		}
		XSType parent = complexType.getBaseType();
		if (parent != schemaSet.getAnyType() && parent.isComplexType()) {
			processComplexType(parent.asComplexType(), elementName, schemaSet, messageType, processedXmlObjects);
		}

		if (complexType.getAttributeUses() != null) {
			processAttributes(complexType, messageType, processedXmlObjects);
		}

		boolean isExtension = complexType.getExplicitContent() == null ? false : true;

		if (complexType.getContentType().asParticle() != null) {
			XSParticle particle = complexType.getContentType().asParticle();

			XSTerm term = particle.getTerm();
			XSModelGroupDecl modelGroupDecl = null;
			XSModelGroup modelGroup = null;
			modelGroup = getModelGroup(modelGroupDecl, term);

			if (modelGroup != null) {
				groupProcessing(modelGroup, particle, messageType, processedXmlObjects, schemaSet, isExtension);
			}

			/*
			 * if (particle.getTerm() != null && particle.getTerm().asModelGroup() != null) { processModelGroup(particle.getTerm().asModelGroup(), schemaSet,
			 * protobufMessage);
			 * 
			 * } else if (particle.getTerm() != null && particle.getTerm().asModelGroupDecl() != null) {
			 * processModelGroup(particle.getTerm().asModelGroupDecl().getModelGroup(), schemaSet, protobufMessage); }
			 */
		} else if (complexType.getContentType().asSimpleType() != null) {
			XSSimpleType xsSimpleType = complexType.getContentType().asSimpleType();

			if (isBaseLevel) { // Only add simpleContent from concrete type?
				/*
				 * if(!processedXmlObjects.contains(xsSimpleType)) processedXmlObjects.add(xsSimpleType);
				 */
				String name = xsSimpleType.getName();
				if (name == null) {
					// Add simple field
					boolean extension = false;
					List<OptionElement> optionElements = new ArrayList<>();
					Options fieldOptions = new Options(Options.FIELD_OPTIONS, optionElements);
					int tag = messageType.getNextFieldNum();
					// Label label = attr. ? Label.REPEATED : null;
					Location fieldLocation = getLocation(xsSimpleType);

					String simpleTypeName = findFieldType(xsSimpleType);

					Field field = new Field(
							NamespaceHelper.xmlNamespaceToProtoFieldPackagename(xsSimpleType.getTargetNamespace(), configuration.forceProtoPackage),
							fieldLocation, null, "value", "SimpleContent value of element", tag, null, simpleTypeName, fieldOptions, extension, true);
					addField(messageType, field, false);

				} else if (basicTypes.contains(xsSimpleType.getName())) {

					// Add simple field
					boolean extension = false;
					List<OptionElement> optionElements = new ArrayList<>();
					Options fieldOptions = new Options(Options.FIELD_OPTIONS, optionElements);
					int tag = messageType.getNextFieldNum();
					// Label label = attr. ? Label.REPEATED : null;
					Location fieldLocation = getLocation(xsSimpleType);

					Field field = new Field(
							NamespaceHelper.xmlNamespaceToProtoFieldPackagename(xsSimpleType.getTargetNamespace(), configuration.forceProtoPackage),
							fieldLocation, null, "value", "SimpleContent value of element", tag, null, xsSimpleType.getName(), fieldOptions, extension, true);
					addField(messageType, field, false);
					// protobufMessage.addField(xsSimpleType.getName(), xsSimpleType.getName(), false, null, resolveDocumentationAnnotation(complexType),
					// xsdMapping);
				} else {
					XSSimpleType primitiveType = xsSimpleType.getPrimitiveType();
					if (primitiveType != null) {

						// Add simple field
						boolean extension = false;
						List<OptionElement> optionElements = new ArrayList<>();
						Options fieldOptions = new Options(Options.FIELD_OPTIONS, optionElements);
						int tag = messageType.getNextFieldNum();
						// Label label = attr. ? Label.REPEATED : null;

						Location fieldLocation = getLocation(xsSimpleType);

						Field field = new Field(
								NamespaceHelper.xmlNamespaceToProtoFieldPackagename(xsSimpleType.getTargetNamespace(), configuration.forceProtoPackage),
								fieldLocation, null, "value", "SimpleContent value of element", tag, null, primitiveType.getName(), fieldOptions, extension,
								true);
						addField(messageType, field, false);

						// protobufMessage.addField(primitiveType.getName(), primitiveType.getTargetNamespace(), primitiveType.getName(), false, null,
						// resolveDocumentationAnnotation(complexType), xsdMapping);
					}
				}
			}
		}

		nestlevel--;
		return typeName;

	}

	private Location getLocation(XSComponent t) {
		Locator l = t.getLocator();
		return new Location("", l.getSystemId(), l.getLineNumber(), l.getColumnNumber());
	}

	private void processAttributes(XSAttContainer complexType, MessageType messageType, Set<Object> processedXmlObjects) {
		Iterator<? extends XSAttributeUse> iterator = complexType.iterateDeclaredAttributeUses();
		// Collection attributes = complexType.getAttributeUses();
		// Iterator<XSAttributeUse> iterator = attributes.iterator();
		while (iterator.hasNext()) {
			XSAttributeUse attr = iterator.next();
			processAttribute(messageType, processedXmlObjects, attr);
		}

		Iterator<? extends XSAttGroupDecl> iterateAttGroups = complexType.iterateAttGroups();
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

					String enumName = createEnum(fieldName, decl.getType().asRestriction(), messageType);

					boolean extension = false;
					List<OptionElement> optionElements = new ArrayList<OptionElement>();
					Options fieldOptions = new Options(Options.FIELD_OPTIONS, optionElements);
					int tag = messageType.getNextFieldNum();
					// Label label = attr. ? Label.REPEATED : null;
					Location fieldLocation = getLocation(decl);

					Field field = new Field(
							NamespaceHelper.xmlNamespaceToProtoFieldPackagename(decl.getType().getTargetNamespace(), configuration.forceProtoPackage),
							fieldLocation, null, fieldName, doc, tag, null, enumName, fieldOptions, extension, false);
					addField(messageType, field, false);

				} else {

					boolean extension = false;
					List<OptionElement> optionElements = new ArrayList<OptionElement>();
					Options fieldOptions = new Options(Options.FIELD_OPTIONS, optionElements);
					int tag = messageType.getNextFieldNum();
					// Label label = attr. ? Label.REPEATED : null;
					Location fieldLocation = getLocation(decl);

					String typeName = findFieldType(decl.getType());

					String packageName = NamespaceHelper.xmlNamespaceToProtoFieldPackagename(decl.getType().getTargetNamespace(),
							configuration.forceProtoPackage);

					Field field = new Field(basicTypes.contains(typeName) ? null : packageName, fieldLocation, null, fieldName, doc, tag, null, typeName,
							fieldOptions, extension, false);
					addField(messageType, field, false);

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

	private void groupProcessing(XSModelGroup modelGroup, XSParticle particle, MessageType messageType, Set<Object> processedXmlObjects, XSSchemaSet schemaSet,
			boolean isExtension) throws ConversionException {
		XSModelGroup.Compositor compositor = modelGroup.getCompositor();

		// We handle "all" and "sequence", but not "choice"
		// if (compositor.equals(XSModelGroup.ALL) || compositor.equals(XSModelGroup.SEQUENCE)) {

		XSParticle[] children = modelGroup.getChildren();
		XSTerm currTerm;
		for (int i = 0; i < children.length; i++) {
			currTerm = children[i].getTerm();
			if (currTerm.isModelGroup()) {
				groupProcessing(currTerm.asModelGroup(), particle, messageType, processedXmlObjects, schemaSet, isExtension);
			} else {
				// Create the new complex type for root types
				navigateSubTypes(children[i], messageType, processedXmlObjects, schemaSet, isExtension);
			}
		}
//		} else if (compositor.equals(XSModelGroup.CHOICE)) {
//			throw new ConversionException("no choice support");
//		}
		messageType.advanceFieldNum();

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

	private int anonymousCounter = 0;

	/*
	 * private TypeDefinition loadSimpleType( XSSimpleType simpleType, boolean lazy, TypeDefinition lazyType ) {
	 * 
	 * if ( simpleType.isRestriction() ) { XSRestrictionSimpleType restriction = simpleType.asRestriction(); checkType( restriction.getBaseType() ); jolietype =
	 * new TypeInlineDefinition( parsingContext, simpleType.getName().replace("-","_") + TYPE_SUFFIX, XsdUtils.xsdToNativeType(
	 * restriction.getBaseType().getName() ), Constants.RANGE_ONE_TO_ONE );
	 * 
	 * } else { log( Level.WARNING, "SimpleType not processed:" + simpleType.getName() ); jolietype = new TypeInlineDefinition( parsingContext,
	 * simpleType.getName().replace("-","_"), NativeType.VOID, Constants.RANGE_ONE_TO_ONE );
	 * 
	 * }
	 * 
	 * return jolietype; }
	 */

	/*
	 * private TypeDefinition loadComplexType( XSComplexType complexType, boolean lazy, TypeDefinition lazyType ) throws ConversionException { XSParticle
	 * particle; XSContentType contentType; contentType = complexType.getContentType();
	 * 
	 * if ( (particle = contentType.asParticle()) == null ) { return null;//createAnyOrUndefined( complexType.getName(), complexType );
	 * 
	 * }
	 * 
	 * TypeInlineDefinition jolieType;
	 * 
	 * if ( lazy ) { jolieType = (TypeInlineDefinition) lazyType; } else { jolieType = createComplexType( complexType, complexType.getName().replace("-","_") +
	 * TYPE_SUFFIX, particle ); }
	 * 
	 * if ( contentType.asSimpleType() != null ) { checkStrictModeForSimpleType( contentType );
	 * 
	 * } else if ( (particle = contentType.asParticle()) != null ) { XSTerm term = particle.getTerm(); XSModelGroupDecl modelGroupDecl = null; XSModelGroup
	 * modelGroup = null; modelGroup = getModelGroup( modelGroupDecl, term );
	 * 
	 * 
	 * if ( modelGroup != null ) { groupProcessing( modelGroup, particle, jolieType ); } } return jolieType;
	 * 
	 * 
	 * }
	 */

	/**
	 * @return
	 */
	private String generateAnonymousName() {
		anonymousCounter++;
		return String.format("Anonymous%03d", anonymousCounter);
	}

	private String createEnum(String elementName, XSRestrictionSimpleType type, MessageType enclosingType) {
		Iterator<? extends XSFacet> it;

		String typeNameToUse = null;

		if (type.getName() != null) {
			typeNameToUse = type.getName();
			enclosingType = null;
		} else {
			typeNameToUse = elementName + "Type";
		}

		Type protoType = getType(type.getTargetNamespace(), typeNameToUse);
		if (protoType == null) {

			type = type.asRestriction();

			Location location = getLocation(type);

			List<EnumConstant> constants = new ArrayList<EnumConstant>();
			it = type.getDeclaredFacets().iterator();

			/*
			 * String enumValuePrefix = CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, typeName) + "_"; List<OptionElement> optionElementsUnspecified =
			 * new ArrayList<>(); constants.add(new EnumConstant(location, enumValuePrefix + "UNSPECIFIED", 0, "Default", new
			 * Options(Options.ENUM_VALUE_OPTIONS, optionElementsUnspecified)));
			 */
			int counter = 1;
			Set<String> addedValues = new HashSet<>();
			while (it.hasNext()) {
				List<OptionElement> optionElements = new ArrayList<>();
				XSFacet next = it.next();
				String doc = resolveDocumentationAnnotation(next);
				String enumValue = next.getValue().value;

				/* String enumConstant = enumValuePrefix + CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, enumValue); */
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
