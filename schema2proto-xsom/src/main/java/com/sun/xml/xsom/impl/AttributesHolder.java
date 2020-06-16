/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.xsom.impl;

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

import com.sun.xml.xsom.XSAttGroupDecl;
import com.sun.xml.xsom.XSAttributeUse;
import com.sun.xml.xsom.impl.parser.SchemaDocumentImpl;
import com.sun.xml.xsom.impl.scd.Iterators;
import com.sun.xml.xsom.impl.Ref.AttGroup;
import org.xml.sax.Locator;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.LinkedHashMap;

public abstract class AttributesHolder extends DeclarationImpl {

    protected AttributesHolder( SchemaDocumentImpl _parent, AnnotationImpl _annon,
                                Locator loc, ForeignAttributesImpl _fa, String _name, boolean _anonymous ) {

        super(_parent,_annon,loc,_fa,_parent.getTargetNamespace(),_name,_anonymous);
    }

    /** set the local wildcard. */
    public abstract void setWildcard(WildcardImpl wc);

    /**
     * Local attribute use.
     * Use linked hash map to guarantee the iteration order, and make it close to
     * what was in the schema document.
     */
    protected final Map<UName,AttributeUseImpl> attributes = new LinkedHashMap<UName,AttributeUseImpl>();
    public void addAttributeUse( UName name, AttributeUseImpl a ) {
        attributes.put( name, a );
    }
    /** prohibited attributes. */
    protected final Set<UName> prohibitedAtts = new LinkedHashSet<UName>();
    public void addProhibitedAttribute( UName name ) {
        prohibitedAtts.add(name);
    }

    /**
     * Returns the attribute uses by looking at attribute groups and etc.
     * Searching for the base type is done in {@link ComplexTypeImpl}.
     */
    public Collection<XSAttributeUse> getAttributeUses() {
        // TODO: this is fairly inefficient
        List<XSAttributeUse> v = new ArrayList<XSAttributeUse>();
        v.addAll(attributes.values());
        for( XSAttGroupDecl agd : getAttGroups() )
            v.addAll(agd.getAttributeUses());
        return v;
    }
    public Iterator<XSAttributeUse> iterateAttributeUses() {
        return getAttributeUses().iterator();
    }



    public XSAttributeUse getDeclaredAttributeUse( String nsURI, String localName ) {
        return attributes.get(new UName(nsURI,localName));
    }

    public Iterator<AttributeUseImpl> iterateDeclaredAttributeUses() {
        return attributes.values().iterator();
    }

    public Collection<AttributeUseImpl> getDeclaredAttributeUses() {
        return attributes.values();
    }


    /** {@link Ref.AttGroup}s that are directly refered from this. */
    protected final Set<Ref.AttGroup> attGroups = new LinkedHashSet<Ref.AttGroup>();

    public void addAttGroup( Ref.AttGroup a ) { attGroups.add(a); }

    // Iterates all AttGroups which are directly referenced from this component
    // this does not iterate att groups referenced from the base type
    public Iterator<XSAttGroupDecl> iterateAttGroups() {
        return new Iterators.Adapter<XSAttGroupDecl,Ref.AttGroup>(attGroups.iterator()) {
            protected XSAttGroupDecl filter(AttGroup u) {
                return u.get();
            }
        };
    }

    public Set<XSAttGroupDecl> getAttGroups() {
        return new AbstractSet<XSAttGroupDecl>() {
            public Iterator<XSAttGroupDecl> iterator() {
                return iterateAttGroups();
            }

            public int size() {
                return attGroups.size();
            }
        };
    }
}
