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

import com.sun.xml.xsom.XSAnnotation;
import com.sun.xml.xsom.parser.AnnotationContext;
import com.sun.xml.xsom.parser.AnnotationParser;
import com.sun.xml.xsom.parser.AnnotationParserFactory;
import javax.xml.XMLConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;

/**
 * {@link AnnotationParserFactory} that parses annotations into a W3C DOM.
 *
 * <p>
 * If you use this parser factory, you'll get {@link Element} that represents
 * {@code <xs:annotation>} from {@link XSAnnotation#getAnnotation()}.
 *
 * <p>
 * When multiple {@code <xs:annotation>s} are found for the given schema component,
 * you'll see all {@code <xs:appinfo>s} and {@code <xs:documentation>s} combined under
 * one {@code <xs:annotation>} element.
 *
 * @author Kohsuke Kawaguchi
 */
public class DomAnnotationParserFactory implements AnnotationParserFactory {
    
    public AnnotationParser create() {
        return new AnnotationParserImpl();
    }

    public AnnotationParser create(boolean disableSecureProcessing) {
        return new AnnotationParserImpl(disableSecureProcessing);
    }
    
    private static final ContextClassloaderLocal<SAXTransformerFactory> stf = new ContextClassloaderLocal<SAXTransformerFactory>() {
        @Override
        protected SAXTransformerFactory initialValue() throws Exception {
            return (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        }
    };

    private static class AnnotationParserImpl extends AnnotationParser {

        /**
         * Identity transformer used to parse SAX into DOM.
         */
        private final TransformerHandler transformer;
        private DOMResult result;

        AnnotationParserImpl() {
            this(false);
        }

        AnnotationParserImpl(boolean disableSecureProcessing) {
            try {
                SAXTransformerFactory factory = stf.get();
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, disableSecureProcessing);
                transformer = factory.newTransformerHandler();
            } catch (TransformerConfigurationException e) {
                throw new Error(e); // impossible
            }
        }
        
        public ContentHandler getContentHandler(AnnotationContext context, String parentElementName, ErrorHandler errorHandler, EntityResolver entityResolver) {
            result = new DOMResult();
            transformer.setResult(result);
            return transformer;
        }

        public Object getResult(Object existing) {
            Document dom = (Document)result.getNode();
            Element e = dom.getDocumentElement();
            if(existing instanceof Element) {
                // merge all the children
                Element prev = (Element) existing;
                Node anchor = e.getFirstChild();
                while(prev.getFirstChild()!=null) {
                    Node move = prev.getFirstChild();
                    e.insertBefore(e.getOwnerDocument().adoptNode(move), anchor );
                }
            }
            return e;
        }
    }
}
