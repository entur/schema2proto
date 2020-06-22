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

/**
 * Base interface for {@link XSComplexType} and {@link XSSimpleType}.
 * 
 * @author
 *  Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public interface XSType extends XSDeclaration {
    /**
     * Returns the base type of this type.
     *
     * Note that if this type represents {@code xs:anyType}, this method returns itself.
     * This is awkward as an API, but it follows the schema specification.
     *
     * @return  always non-null.
     */
    XSType getBaseType();

    final static int EXTENSION = 1;
    final static int RESTRICTION = 2;
    final static int SUBSTITUTION = 4;

    int getDerivationMethod();

    /** Returns true if {@code this instanceof XSSimpleType}. */
    boolean isSimpleType();
    /** Returns true if {@code this instanceof XSComplexType}. */
    boolean isComplexType();

    /**
     * Lists up types that can substitute this type by using xsi:type.
     * Includes this type itself.
     * <p>
     * This method honors the block flag.
     */
    XSType[] listSubstitutables();

    /**
     * If this {@link XSType} is redefined by another type,
     * return that component.
     *
     * @return null
     *      if this component has not been redefined.
     */
    XSType getRedefinedBy();

    /**
     * Returns the number of complex types that redefine this component.
     *
     * <p>
     * For example, if A is redefined by B and B is redefined by C,
     * A.getRedefinedCount()==2, B.getRedefinedCount()==1, and
     * C.getRedefinedCount()==0.
     */
    int getRedefinedCount();


    /** Casts this object to XSSimpleType if possible, otherwise returns null. */
    XSSimpleType asSimpleType();
    /** Casts this object to XSComplexType if possible, otherwise returns null. */
    XSComplexType asComplexType();

    /**
     * Returns true if this type is derived from the specified type.
     *
     * <p>
     * Note that {@code t.isDerivedFrom(t)} returns true.
     */
    boolean isDerivedFrom( XSType t );
}
