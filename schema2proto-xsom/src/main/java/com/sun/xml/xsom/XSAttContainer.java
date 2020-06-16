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

/**
 * Common aspect of {@link XSComplexType} and {@link XSAttGroupDecl}
 * as the container of attribute uses/attribute groups.
 * 
 * @author
 *     Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public interface XSAttContainer extends XSDeclaration {
    XSWildcard getAttributeWildcard();
    
    /**
     * Looks for the attribute use with the specified name from
     * all the attribute uses that are directly/indirectly
     * referenced from this component.
     * 
     * <p>
     * This is the exact implementation of the "attribute use"
     * schema component.
     */
    XSAttributeUse getAttributeUse( String nsURI, String localName );
    
    /**
     * Lists all the attribute uses that are directly/indirectly
     * referenced from this component.
     * 
     * <p>
     * This is the exact implementation of the "attribute use"
     * schema component.
     */
    Iterator<? extends XSAttributeUse> iterateAttributeUses();

    /**
     * Gets all the attribute uses.
     */
    Collection<? extends XSAttributeUse> getAttributeUses();

    /**
     * Looks for the attribute use with the specified name from
     * the attribute uses which are declared in this complex type.
     * 
     * This does not include att uses declared in att groups that
     * are referenced from this complex type, nor does include
     * att uses declared in base types.
     */
    XSAttributeUse getDeclaredAttributeUse( String nsURI, String localName );
    
    /**
     * Lists all the attribute uses that are declared in this complex type.
     */
    Iterator<? extends XSAttributeUse> iterateDeclaredAttributeUses();

    /**
     * Lists all the attribute uses that are declared in this complex type.
     */
    Collection<? extends XSAttributeUse> getDeclaredAttributeUses();


    /**
     * Iterates all AttGroups which are directly referenced from
     * this component.
     */
    Iterator<? extends XSAttGroupDecl> iterateAttGroups();

    /**
     * Iterates all AttGroups which are directly referenced from
     * this component.
     */
    Collection<? extends XSAttGroupDecl> getAttGroups();
}
