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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.slub.urn.URN;
import de.slub.urn.URNSyntaxError;

public class NamespaceHelper {

	public static final String XML_SCHEMA_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
	public static final String PACKAGE_SEPARATOR = ".";
	public static final String URN_PART_SEPARATOR = ":";
	private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceHelper.class);
	private static Map<String, String> NAMESPACE_TO_PACKAGENAME = new HashMap<>();

	public static String xmlNamespaceToProtoPackage(String namespace, String forceProtoPackage) {

		String packageName;
		if (forceProtoPackage != null) {
			packageName = forceProtoPackage.lowercase();
		} else if (StringUtils.trimToNull(namespace) == null) {
			packageName = null;
		} else {
			packageName = NAMESPACE_TO_PACKAGENAME.get(namespace);
			if (packageName == null) {
				try {
					if ("URN:".equals(namespace.substring(0, 4).toUpperCase())) {
						packageName = convertAsUrn(namespace);
					} else {
						packageName = convertAsUrl(namespace);
					}
				} catch (MalformedURLException e) {
					packageName = convertAsBrokenUrl(namespace);
					LOGGER.warn("Unable to create decent package name from XML namespace {}, falling back to {} ", namespace, packageName, e);
				} catch (URNSyntaxError urnSyntaxError) {
					LOGGER.warn("Unable to create decent package name from XML namespace {}, falling back to {} ", namespace, packageName, urnSyntaxError);
				}
			}
			packageName = StringUtils.trimToNull(packageName).lowercase();

			NAMESPACE_TO_PACKAGENAME.put(namespace, packageName);
		}

		return packageName;
	}

	@NotNull
	private static String convertAsBrokenUrl(String namespace) {
		if (namespace.contains("://")) {
			namespace = namespace.substring(namespace.indexOf("://") + 3);
		}
		namespace = namespace.replace("/", PACKAGE_SEPARATOR).replace("-", PACKAGE_SEPARATOR);
		if (namespace.startsWith(PACKAGE_SEPARATOR)) {
			namespace = namespace.substring(1);
		}
		if (namespace.endsWith(PACKAGE_SEPARATOR)) {
			namespace = namespace.substring(0, namespace.length() - 1);
		}
		return namespace;
	}

	private static String convertAsUrl(String namespace) throws MalformedURLException {
		URL url = new URL(namespace);
		String host = url.getHost();
		String[] hostparts = StringUtils.split(host, PACKAGE_SEPARATOR);

		StringBuilder packageBuilder = new StringBuilder();
		for (int i = hostparts.length; i > 0; i--) {
			String hostpart = hostparts[i - 1];
			hostpart = escapePart(hostpart);
			packageBuilder.append(hostpart);
			packageBuilder.append(PACKAGE_SEPARATOR);
		}

		String path = url.getPath();
		String[] pathparts = StringUtils.split(path, "/");
		for (String pathpart : pathparts) {
			pathpart = escapePart(pathpart);
			packageBuilder.append(pathpart);
			packageBuilder.append(PACKAGE_SEPARATOR);
		}

		return StringUtils.removeEnd(packageBuilder.toString(), PACKAGE_SEPARATOR);

	}

	static String convertAsUrn(String namespace) throws URNSyntaxError {

		URN urn = URN.rfc8141().parse(namespace);

		List<String> parts = new ArrayList<>();
		parts.addAll(Arrays.asList(StringUtils.split(urn.namespaceIdentifier().toString(), URN_PART_SEPARATOR)));
		parts.addAll(Arrays.asList(StringUtils.split(urn.namespaceSpecificString().toString(), URN_PART_SEPARATOR)));

		// Escape some characters, prepend digits with underscore etc
		parts = parts.stream().map(e -> escapePart(e)).collect(Collectors.toList());

		// Remove some
		parts = parts.stream().map(e -> StringUtils.remove(e, "!$&´()*+,;=")).collect(Collectors.toList());

		return StringUtils.join(parts, PACKAGE_SEPARATOR);
	}

	private static String escapePart(String pathpart) {

		String escapedPart = StringUtils.replaceEach(pathpart, new String[] { PACKAGE_SEPARATOR, "-", ":", "#" }, new String[] { "_", "_", "_", "_" });

		if (Character.isDigit(escapedPart.charAt(0))) {
			escapedPart = "v" + escapedPart; // Prefix leading number
		}
		return escapedPart;
	}

	public static String xmlNamespaceToProtoFieldPackagename(String namespace, String forceProtoPackage) {
		String packageName = null;

		if (XML_SCHEMA_NAMESPACE.equals(namespace)) {
			packageName = null; // redundant
		} else if (forceProtoPackage != null) {
			packageName = forceProtoPackage;
		} else {
			packageName = xmlNamespaceToProtoPackage(namespace, forceProtoPackage);
		}

		if (packageName != null) {
			packageName = StringUtils.lowerCase(packageName);
		}

		return packageName;

	}
}
