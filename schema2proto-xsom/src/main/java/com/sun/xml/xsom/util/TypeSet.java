/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.xsom.util;

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

import com.sun.xml.xsom.XSType;

/**
 * A simple abstraction for a set of Types that defines containment functions.
 * 
 * @author <a href="mailto:Ryan.Shoemaker@Sun.COM">Ryan Shoemaker</a>, Sun Microsystems, Inc.
 */
public abstract class TypeSet {

    /**
     * Return true if this TypeSet contains the specified type.
     * 
     * Concrete implementations of this method determine what it
     * means for the TypeSet to "contain" a type.
     *  
     * @param type the type
     * @return true iff this TypeSet contains the specified type
     */
    public abstract boolean contains(XSType type);

    /**
     * Calculate the TypeSet formed by the intersection of two
     * other TypeSet objects.
     * 
     * @param a a TypeSet
     * @param b another TypeSet
     * @return the intersection of a and b
     */
    public static TypeSet intersection(final TypeSet a, final TypeSet b) {
        return new TypeSet(){
            public boolean contains(XSType type) {
                return a.contains(type) && b.contains(type);
            }
        };
    }

    /**
     * Calculate the TypeSet formed by the union of two
     * other TypeSet objects.
     * 
     * @param a a TypeSet
     * @param b another TypeSet
     * @return the union of a and b
     */
    public static TypeSet union(final TypeSet a, final TypeSet b) {
        return new TypeSet(){
            public boolean contains(XSType type) {
                return a.contains(type) || b.contains(type);
            }
        };
    }
}
