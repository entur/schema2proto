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
/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.XSComponent;

import java.util.Collection;

/**
 * @author Kohsuke Kawaguchi
 */
public class SCDTest extends AbstractXSOMTest {
    /**
     * Taken from spec examples in section 4.2.16
     */
    public void testSpec() throws Exception {
        XSSchemaSet s = load("scdtest.xsd");

        MapNamespaceContext nsc = new MapNamespaceContext("", "", "my", "tns");

        assertOne("foo-bar element declaration",s.select("/my:foo-bar",nsc));
        assertOne("articleType complex type",s.select("type::my:articleType",nsc));
        assertOne("articleType complex type",s.select("/type::my:articleType",nsc));
        assertOne("section element declaration", s.select("/type::my:articleType/model::sequence/element::my:section",nsc));
        assertOne("appendix element declaration",s.select("/type::my:articleType/model::sequence/element::my:appendix",nsc));
        assertOne("anonymous complex type",s.select("/element::my:chapter/type::0",nsc));
        assertOne("wildcard",s.select("/element::my:chapter/type::0/model::sequence/any::*",nsc));
        assertOne("name attribute declaration",s.select("/element::my:chapter/type::0/attribute::name",nsc));
    }

    private void assertOne(String name, Collection<XSComponent> r) {
        assertEquals(1,r.size());
        assertEquals(name,r.iterator().next().toString());
    }
}
