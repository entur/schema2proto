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
import com.sun.xml.xsom.XSWildcard;
import com.sun.xml.xsom.impl.parser.DelayedRef;
import com.sun.xml.xsom.impl.parser.SchemaDocumentImpl;
import com.sun.xml.xsom.visitor.XSFunction;
import com.sun.xml.xsom.visitor.XSVisitor;
import org.xml.sax.Locator;

import java.util.Iterator;

public class AttGroupDeclImpl extends AttributesHolder implements XSAttGroupDecl
{
    public AttGroupDeclImpl( SchemaDocumentImpl _parent, AnnotationImpl _annon,
        Locator _loc, ForeignAttributesImpl _fa, String _name, WildcardImpl _wildcard ) {
        
        this(_parent,_annon,_loc,_fa,_name);
        setWildcard(_wildcard);
    }
        
    public AttGroupDeclImpl( SchemaDocumentImpl _parent, AnnotationImpl _annon,
        Locator _loc, ForeignAttributesImpl _fa, String _name ) {
            
        super(_parent,_annon,_loc,_fa,_name,false);
    }
    
    
    private WildcardImpl wildcard;
    public void setWildcard( WildcardImpl wc ) { wildcard=wc; }
    public XSWildcard getAttributeWildcard() { return wildcard; }

    public XSAttributeUse getAttributeUse( String nsURI, String localName ) {
        UName name = new UName(nsURI,localName);
        XSAttributeUse o=null;
        
        Iterator itr = iterateAttGroups();
        while(itr.hasNext() && o==null)
            o = ((XSAttGroupDecl)itr.next()).getAttributeUse(nsURI,localName);
        
        if(o==null)     o = attributes.get(name);
        
        return o;
    }
    
    public void redefine( AttGroupDeclImpl ag ) {
        for (Iterator itr = attGroups.iterator(); itr.hasNext();) {
            DelayedRef.AttGroup r = (DelayedRef.AttGroup) itr.next();
            r.redefine(ag);
        }
    }
    
    public void visit( XSVisitor visitor ) {
        visitor.attGroupDecl(this);
    }
    public Object apply( XSFunction function ) {
        return function.attGroupDecl(this);
    }
}
