/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.xsom;

/*-
 * #%L
 * XSOM
 * %%
 * Copyright (C) 2019 - 2020 Entur
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

import com.sun.tools.rngdatatype.ValidationContext;

/**
 * String with in-scope namespace binding information.
 *
 * <p>
 * In a general case, text (PCDATA/attributes) that appear in XML schema
 * cannot be correctly interpreted unless you also have in-scope namespace
 * binding (a case in point is QName.) Therefore, it's convenient to
 * handle the lexical representation and the in-scope namespace binding
 * in a pair.
 *
 * @author Kohsuke Kawaguchi
 */
public final class XmlString {
    /**
     * Textual value. AKA lexical representation.
     */
    public final String value;

    /**
     * Used to resole in-scope namespace bindings.
     */
    public final ValidationContext context;

    /**
     * Creates a new {@link XmlString} from a lexical representation and in-scope namespaces.
     */
    public XmlString(String value, ValidationContext context) {
        this.value = value;
        this.context = context;
        if(context==null)
            throw new IllegalArgumentException();
    }

    /**
     * Creates a new {@link XmlString} with empty in-scope namespace bindings.
     */
    public XmlString(String value) {
        this(value,NULL_CONTEXT);
    }

    /**
     * Resolves a namespace prefix to the corresponding namespace URI.
     *
     * This method is used for resolving prefixes in the {@link #value}
     * (such as when {@link #value} represents a QName type.)
     *
     * <p>
     * If the prefix is "" (empty string), the method
     * returns the default namespace URI.
     *
     * <p>
     * If the prefix is "xml", then the method returns
     * "http://www.w3.org/XML/1998/namespace",
     * as defined in the XML Namespaces Recommendation.
     *
     * @return
     *		namespace URI of this prefix.
     *		If the specified prefix is not declared,
     *		the implementation returns null.
     */
    public final String resolvePrefix(String prefix) {
        return context.resolveNamespacePrefix(prefix);
    }

    public String toString() {
        return value;
    }

    private static final ValidationContext NULL_CONTEXT = new ValidationContext() {
        public String resolveNamespacePrefix(String s) {
            if(s.length()==0)   return "";
            if(s.equals("xml")) return "http://www.w3.org/XML/1998/namespace";
            return null;
        }

        public String getBaseUri() {
            return null;
        }

        public boolean isUnparsedEntity(String s) {
            return false;
        }

        public boolean isNotation(String s) {
            return false;
        }
    };
}
