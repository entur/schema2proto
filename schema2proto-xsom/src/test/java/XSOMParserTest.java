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

/*
 * XSOMParserTest.java
 * JUnit based test
 *
 * Created on April 13, 2006, 9:54 AM
 */

import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.parser.XSOMParser;
import com.sun.xml.xsom.parser.SchemaDocument;
import junit.framework.*;
import org.xml.sax.InputSource;

import java.net.URL;
import java.util.Set;

/**
 *
 * @author Farrukh S. Najmi
 */
public class XSOMParserTest extends TestCase {

    //private static String docURLStr = "http://docs.oasis-open.org/regrep/v3.0/schema/lcm.xsd";
    //private static String docURLStr = "http://ebxmlrr.sourceforge.net/private/sun/irs/ContactMechanism/IRS-ContactMechanismCommonAggregateComponents-1.0.xsd";
    private static XSOMParser instance = null;

    public XSOMParserTest(String testName) {
        super(testName);
    }

    protected void setUp() throws Exception {
        instance = new XSOMParser();
    }

    protected void tearDown() throws Exception {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(XSOMParserTest.class);

        return suite;
    }

    /**
     * Test of parse method, of class com.sun.xml.xsom.parser.XSOMParser.
     */
    public void testParse() throws Exception {
        System.out.println("parse");

        instance.parse(getClass().getResource("/lcm.xsd"));
    }

    /**
     * Test of getDocuments method, of class com.sun.xml.xsom.parser.XSOMParser.
     */
    public void testGetDocuments() {
        System.out.println("getDocuments");


        Set<SchemaDocument> documents = instance.getDocuments();
        for (SchemaDocument doc : documents) {
            System.out.println("Schema document: "+doc.getSystemId());
            System.out.println("  target namespace: "+doc.getTargetNamespace());
            for (SchemaDocument ref : doc.getReferencedDocuments()) {
                System.out.print("    -> "+ref.getSystemId());
                if(doc.includes(ref))
                    System.out.print(" (include)");
                System.out.println();
            }
        }

    }

    /**
     * Test of getResult method, of class com.sun.xml.xsom.parser.XSOMParser.
     */
    public void testGetResult() throws Exception {
        System.out.println("getResult");

        XSSchemaSet result = instance.getResult();
    }


}
