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

import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import com.sun.tools.rngdatatype.ValidationContext;

/**
 * Foreign attributes on schema elements.
 *
 * <p>
 * This is not a schema component as defined in the spec,
 * but this is often useful for a schema processing application.
 *
 * @author Kohsuke Kawaguchi
 */
public interface ForeignAttributes extends Attributes {
    /**
     * Returns context information of the element to which foreign attributes
     * are attached.
     *
     * <p>
     * For example, this can be used to resolve relative references to other resources
     * (by using {@link ValidationContext#getBaseUri()}) or to resolve
     * namespace prefixes in the attribute values (by using {@link ValidationContext#resolveNamespacePrefix(String)}.
     *
     * @return
     *      always non-null.
     */
    ValidationContext getContext();

    /**
     * Returns the location of the element to which foreign attributes
     * are attached.
     */
    Locator getLocator();
}
