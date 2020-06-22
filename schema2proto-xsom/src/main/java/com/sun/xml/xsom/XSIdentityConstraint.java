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

import java.util.List;

/**
 * Identity constraint.
 *
 * @author Kohsuke Kawaguchi
 */
public interface XSIdentityConstraint extends XSComponent {

    /**
     * Gets the {@link XSElementDecl} that owns this identity constraint.
     *
     * @return
     *      never null.
     */
    XSElementDecl getParent();

    /**
     * Name of the identity constraint.
     *
     * A name uniquely identifies this {@link XSIdentityConstraint} within
     * the namespace.
     *
     * @return
     *      never null.
     */
    String getName();

    /**
     * Target namespace of the identity constraint.
     *
     * Just short for <code>getParent().getTargetNamespace()</code>.
     */
    String getTargetNamespace();

    /**
     * Returns the type of the identity constraint.
     *
     * @return
     *      either {@link #KEY},{@link #KEYREF}, or {@link #UNIQUE}.
     */
    short getCategory();

    final short KEY = 0;
    final short KEYREF = 1;
    final short UNIQUE = 2;

    /**
     * Returns the selector XPath expression as string.
     *
     * @return
     *      never null.
     */
    XSXPath getSelector();

    /**
     * Returns the list of field XPaths.
     *
     * @return
     *      a non-empty read-only list of {@link String}s,
     *      each representing the XPath.
     */
    List<XSXPath> getFields();

    /**
     * If this is {@link #KEYREF}, returns the key {@link XSIdentityConstraint}
     * being referenced.
     *
     * @return
     *      always non-null (when {@link #getCategory()}=={@link #KEYREF}).
     * @throws IllegalStateException
     *      if {@link #getCategory()}!={@link #KEYREF}
     */
    XSIdentityConstraint getReferencedKey();
}
