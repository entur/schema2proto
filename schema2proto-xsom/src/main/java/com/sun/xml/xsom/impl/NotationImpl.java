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

import com.sun.xml.xsom.XSNotation;
import com.sun.xml.xsom.impl.parser.SchemaDocumentImpl;
import com.sun.xml.xsom.visitor.XSFunction;
import com.sun.xml.xsom.visitor.XSVisitor;
import org.xml.sax.Locator;

/**
 * 
 * @author
 *     Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public class NotationImpl extends DeclarationImpl implements XSNotation {
    
    public NotationImpl( SchemaDocumentImpl owner, AnnotationImpl _annon,
        Locator _loc, ForeignAttributesImpl _fa, String _name,
        String _publicId, String _systemId ) {
        super(owner,_annon,_loc,_fa,owner.getTargetNamespace(),_name,false);
        
        this.publicId = _publicId;
        this.systemId = _systemId;
    }
    
    private final String publicId;
    private final String systemId;
    
    public String getPublicId() { return publicId; }
    public String getSystemId() { return systemId; }

    public void visit(XSVisitor visitor) {
        visitor.notation(this);
    }

    public Object apply(XSFunction function) {
        return function.notation(this);
    }

}
