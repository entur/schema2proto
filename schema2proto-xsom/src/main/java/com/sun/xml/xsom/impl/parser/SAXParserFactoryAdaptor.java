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

import com.sun.xml.xsom.parser.XMLParser;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;
import org.xml.sax.helpers.XMLReaderAdapter;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;


/**
 * {@link SAXParserFactory} implementation that ultimately
 * uses {@link XMLParser} to parse documents.
 * 
 * @deprecated 
 * 
 * @author
 *     Kohsuke Kawaguchi (kohsuke.kawaguchi@sun.com)
 */
public class SAXParserFactoryAdaptor extends SAXParserFactory {
    
    private final XMLParser parser;
    
    public SAXParserFactoryAdaptor( XMLParser _parser ) {
        this.parser = _parser;
    }
    
    public SAXParser newSAXParser() throws ParserConfigurationException, SAXException {
        return new SAXParserImpl();
    }

    public void setFeature(String name, boolean value) {
        throw new UnsupportedOperationException("XSOM parser does not support JAXP features.");
    }

    public boolean getFeature(String name) {
        return false;
    }
    
    private class SAXParserImpl extends SAXParser
    {
        private final XMLReaderImpl reader = new XMLReaderImpl();
        
        /**
         * @deprecated
         */
        public org.xml.sax.Parser getParser() throws SAXException {
            return new XMLReaderAdapter(reader);
        }

        public XMLReader getXMLReader() throws SAXException {
            return reader;
        }

        public boolean isNamespaceAware() {
            return true;
        }

        public boolean isValidating() {
            return false;
        }

        public void setProperty(String name, Object value) {
        }

        public Object getProperty(String name) {
            return null;
        }
    }
    
    private class XMLReaderImpl extends XMLFilterImpl
    {
        public void parse(InputSource input) throws IOException, SAXException {
            parser.parse(input,this,this,this);
        }

        public void parse(String systemId) throws IOException, SAXException {
            parser.parse(new InputSource(systemId),this,this,this);
        }
    }
}
