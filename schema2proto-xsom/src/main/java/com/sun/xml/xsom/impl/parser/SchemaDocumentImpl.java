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

import com.sun.xml.xsom.impl.SchemaImpl;
import com.sun.xml.xsom.parser.SchemaDocument;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * {@link SchemaDocument} implementation.
 *
 * @author Kohsuke Kawaguchi
 */
public final class SchemaDocumentImpl implements SchemaDocument
{
    private final SchemaImpl schema;

    /**
     * URI of the schema document to be parsed. Can be null.
     */
    private final String schemaDocumentURI;

    /**
     * {@link SchemaDocumentImpl}s that are referenced from this document.
     */
    final Set<SchemaDocumentImpl> references = new LinkedHashSet<SchemaDocumentImpl>();

    /**
     * {@link SchemaDocumentImpl}s that are referencing this document.
     */
    final Set<SchemaDocumentImpl> referers = new LinkedHashSet<SchemaDocumentImpl>();

    protected SchemaDocumentImpl(SchemaImpl schema, String _schemaDocumentURI) {
        this.schema = schema;
        this.schemaDocumentURI = _schemaDocumentURI;
    }

    public String getSystemId() {
        return schemaDocumentURI;
    }

    public String getTargetNamespace() {
        return schema.getTargetNamespace();
    }

    public SchemaImpl getSchema() {
        return schema;
    }

    public Set<SchemaDocument> getReferencedDocuments() {
        return Collections.<SchemaDocument>unmodifiableSet(references);
    }

    public Set<SchemaDocument> getIncludedDocuments() {
        return getImportedDocuments(this.getTargetNamespace());
    }

    public Set<SchemaDocument> getImportedDocuments(String targetNamespace) {
        if(targetNamespace==null)
            throw new IllegalArgumentException();
        Set<SchemaDocument> r = new LinkedHashSet<SchemaDocument>();
        for (SchemaDocumentImpl doc : references) {
            if(doc.getTargetNamespace().equals(targetNamespace))
                r.add(doc);
        }
        return Collections.unmodifiableSet(r);
    }

    public boolean includes(SchemaDocument doc) {
        if(!references.contains(doc))
            return false;
        return doc.getSchema()==schema;
    }

    public boolean imports(SchemaDocument doc) {
        if(!references.contains(doc))
            return false;
        return doc.getSchema()!=schema;
    }

    public Set<SchemaDocument> getReferers() {
        return Collections.<SchemaDocument>unmodifiableSet(referers);
    }

    @Override
    public boolean equals(Object o) {
        SchemaDocumentImpl rhs = (SchemaDocumentImpl) o;

        if( this.schemaDocumentURI==null || rhs.schemaDocumentURI==null)
            return this==rhs;
        if(!schemaDocumentURI.equals(rhs.schemaDocumentURI) )
            return false;
        return this.schema==rhs.schema;
    }
    
    @Override
    public int hashCode() {
        if (schemaDocumentURI==null)
            return super.hashCode();
        if (schema == null) {
            return schemaDocumentURI.hashCode();
        }
        return schemaDocumentURI.hashCode()^this.schema.hashCode();
    }
}
