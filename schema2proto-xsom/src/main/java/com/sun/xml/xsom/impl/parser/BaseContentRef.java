/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.xsom.impl.parser;

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

import com.sun.xml.xsom.impl.Ref;
import com.sun.xml.xsom.XSContentType;
import com.sun.xml.xsom.XSType;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

public final class BaseContentRef implements Ref.ContentType, Patch {
    private final Ref.Type baseType;
    private final Locator loc;

    public BaseContentRef(final NGCCRuntimeEx $runtime, Ref.Type _baseType) {
        this.baseType = _baseType;
        $runtime.addPatcher(this);
        $runtime.addErrorChecker(new Patch() {
            public void run() throws SAXException {
                XSType t = baseType.getType();
                if (t.isComplexType() && t.asComplexType().getContentType().asParticle()!=null) {
                    $runtime.reportError(
                        Messages.format(Messages.ERR_SIMPLE_CONTENT_EXPECTED,
                            t.getTargetNamespace(), t.getName()), loc);
                }
            }
        });
        this.loc = $runtime.copyLocator();
    }

    public XSContentType getContentType() {
        XSType t = baseType.getType();
        if(t.asComplexType()!=null)
            return t.asComplexType().getContentType();
        else
            return t.asSimpleType();
    }

    public void run() throws SAXException {
        if (baseType instanceof Patch)
            ((Patch) baseType).run();
    }
}
