package no.entur.schema2proto;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class NamespaceHelper {

	public static final String XML_SCHEMA_NAMESPACE = "http://www.w3.org/2001/XMLSchema";

	public static final String XML_NAMESPACE_NAMESPACE = "http://www.w3.org/XML/1998/namespace";

	private static final Logger LOGGER = LoggerFactory.getLogger(NamespaceHelper.class);

	public static String xmlNamespaceToProtoPackage(String namespace, String forceProtoPackage) {

		if (forceProtoPackage != null) {
			return forceProtoPackage;
		}

		if (namespace == null) {
			return null;
		}

		try {
			namespace = convertAsUrl(namespace);
		} catch (MalformedURLException e) {
			LOGGER.warn("Unable to create decent package name from XML defaultProtoPackage " + namespace, e);
		}

		if (namespace.contains("://")) {
			namespace = namespace.substring(namespace.indexOf("://") + 3);
		}
		namespace = namespace.replaceAll("/", ".").replace("-", ".");
		if (namespace.startsWith("."))
			namespace = namespace.substring(1);
		if (namespace.endsWith("."))
			namespace = namespace.substring(0, namespace.length() - 1);
		return namespace;
	}

	private static String convertAsUrl(String namespace) throws MalformedURLException {
		URL url = new URL(namespace);
		String host = url.getHost();
		String[] hostparts = StringUtils.split(host, ".");

		StringBuilder packageBuilder = new StringBuilder();
		for (int i = hostparts.length; i > 0; i--) {
			packageBuilder.append(hostparts[i - 1]);
			packageBuilder.append(".");
		}

		String path = url.getPath();
		String[] pathparts = StringUtils.split(path, "/");
		for (String pathpart : pathparts) {
			packageBuilder.append(pathpart);
			packageBuilder.append(".");
		}

		return StringUtils.removeEnd(packageBuilder.toString(), ".");

	}

	public static String xmlNamespaceToProtoFieldPackagename(String namespace, String forceProtoPackage) {
		if (XML_SCHEMA_NAMESPACE.equals(namespace) || XML_NAMESPACE_NAMESPACE.equals(namespace)) {
			return null;
		} else if (forceProtoPackage != null) {
			return forceProtoPackage;
		} else {
			return xmlNamespaceToProtoPackage(namespace, forceProtoPackage);
		}

	}
}
