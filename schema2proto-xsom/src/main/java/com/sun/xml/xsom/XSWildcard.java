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

import java.util.Iterator;
import java.util.Collection;

import com.sun.xml.xsom.visitor.XSWildcardFunction;
import com.sun.xml.xsom.visitor.XSWildcardVisitor;

/**
 * Wildcard schema component (used for both attribute wildcard
 * and element wildcard.)
 * 
 * XSWildcard interface can always be downcasted to either
 * Any, Other, or Union.
 */
public interface XSWildcard extends XSComponent, XSTerm
{
    static final int LAX = 1;
    static final int STRTICT = 2;
    static final int SKIP = 3;
    /**
     * Gets the processing mode.
     * 
     * @return
     *      Either LAX, STRICT, or SKIP.
     */
    int getMode();

    /**
     * Returns true if the specified namespace URI is valid
     * wrt this wildcard.
     * 
     * @param namespaceURI
     *      Use the empty string to test the default no-namespace.
     */
    boolean acceptsNamespace(String namespaceURI);

    /** Visitor support. */
    void visit(XSWildcardVisitor visitor);
    <T> T apply(XSWildcardFunction<T> function);

    /**
     * <code>##any</code> wildcard.
     */
    interface Any extends XSWildcard {
    }
    /**
     * <code>##other</code> wildcard.
     */
    interface Other extends XSWildcard {
        /**
         * Gets the namespace URI excluded from this wildcard.
         */
        String getOtherNamespace();
    }
    /**
     * Wildcard of a set of namespace URIs.
     */
    interface Union extends XSWildcard {
        /**
         * Short for <code>getNamespaces().iterator()</code>
         */
        Iterator<String> iterateNamespaces();

        /**
         * Read-only list of namespace URIs.
         */
        Collection<String> getNamespaces();
    }
}
