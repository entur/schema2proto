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
import java.util.Set;

/**
 * Element declaration.
 * 
 * @author
 *  Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public interface XSElementDecl extends XSDeclaration, XSTerm
{
    /**
     * Gets the type of this element declaration.
     * @return
     *      always non-null.
     */
    XSType getType();

    boolean isNillable();

    /**
     * Gets the substitution head of this element, if any.
     * Otherwise null.
     */
    XSElementDecl getSubstAffiliation();

    /**
     * Returns all the {@link XSIdentityConstraint}s in this element decl.
     *
     * @return
     *      never null, but can be empty.
     */
    List<XSIdentityConstraint> getIdentityConstraints();

    /**
     * Checks the substitution excluded property of the schema component.
     * 
     * IOW, this checks the value of the <code>final</code> attribute
     * (plus <code>finalDefault</code>).
     * 
     * @param method
     *      Possible values are {@link XSType#EXTENSION} or
     *      <code>XSType.RESTRICTION</code>.
     */
    boolean isSubstitutionExcluded(int method);

    /**
     * Checks the diallowed substitution property of the schema component.
     * 
     * IOW, this checks the value of the <code>block</code> attribute
     * (plus <code>blockDefault</code>).
     * 
     * @param method
     *      Possible values are {@link XSType#EXTENSION},
     *      <code>XSType.RESTRICTION</code>, or <code>XSType.SUBSTITUTION</code>
     */
    boolean isSubstitutionDisallowed(int method);

    boolean isAbstract();

    /**
     * Returns the element declarations that can substitute
     * this element.
     * 
     * <p>
     * IOW, this set returns all the element decls that satisfies
     * <a href="http://www.w3.org/TR/xmlschema-1/#cos-equiv-derived-ok-rec">
     * the "Substitution Group OK" constraint.
     * </a>
     * 
     * @return
     *      nun-null valid array. The return value always contains this element
     *      decl itself. 
     * 
     * @deprecated
     *      this method allocates a new array every time, so it could be
     *      inefficient when working with a large schema. Use
     *      {@link #getSubstitutables()} instead.
     */
    XSElementDecl[] listSubstitutables();
    
    /**
     * Returns the element declarations that can substitute
     * this element.
     * 
     * <p>
     * IOW, this set returns all the element decls that satisfies
     * <a href="http://www.w3.org/TR/xmlschema-1/#cos-equiv-derived-ok-rec">
     * the "Substitution Group OK" constraint.
     * </a>
     * 
     * <p>
     * Note that the above clause does <em>NOT</em> check for
     * abstract elements. So abstract elements may still show up
     * in the returned set.
     * 
     * @return
     *      nun-null unmodifiable list.
     *      The returned list always contains this element decl itself. 
     */
    Set<? extends XSElementDecl> getSubstitutables();
    
    /**
     * Returns true if this element declaration can be validly substituted
     * by the given declaration.
     * 
     * <p>
     * Just a short cut of {@code getSubstitutables().contain(e);}
     */
    boolean canBeSubstitutedBy(XSElementDecl e);

    // TODO: identitiy constraints
    // TODO: scope

    XmlString getDefaultValue();
    XmlString getFixedValue();

    /**
     * Used for javadoc schema generation
     *
     * @return
     *    null if form attribute not present,
     *    true if form attribute present and set to qualified,
     *    false if form attribute present and set to unqualified.
     */

    Boolean getForm();
}
