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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * {@link Collection} that returns the view of objects which are actually fetched
 * lazily from an {@link Iterator}.
 *
 * @author Kohsuke Kawaguchi
 */
public class DeferedCollection<T> implements Collection<T> {
    /**
     * The iterator that lazily evaluates SCD query.
     */
    private final Iterator<T> result;

    /**
     * Stores values that are already fetched from {@link #result}.
     */
    private final List<T> archive = new ArrayList<T>();

    public DeferedCollection(Iterator<T> result) {
        this.result = result;
    }

    public boolean isEmpty() {
        if(archive.isEmpty())
            fetch();
        return archive.isEmpty();
    }

    public int size() {
        fetchAll();
        return archive.size();
    }

    public boolean contains(Object o) {
        if(archive.contains(o))
            return true;
        while(result.hasNext()) {
            T value = result.next();
            archive.add(value);
            if(value.equals(o))
                return true;
        }
        return false;
    }

    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if(!contains(o))
                return false;
        }
        return true;
    }

    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int idx=0;
            public boolean hasNext() {
                if(idx<archive.size())
                    return true;
                return result.hasNext();
            }

            public T next() {
                if(idx==archive.size())
                    fetch();
                if(idx==archive.size())
                    throw new NoSuchElementException();
                return archive.get(idx++);
            }

            public void remove() {
                // TODO
            }
        };
    }

    public Object[] toArray() {
        fetchAll();
        return archive.toArray();
    }

    public <T>T[] toArray(T[] a) {
        fetchAll();
        return archive.toArray(a);
    }



    private void fetchAll() {
        while(result.hasNext())
            archive.add(result.next());
    }

    /**
     * Fetches another item from {@link
     */
    private void fetch() {
        if(result.hasNext())
            archive.add(result.next());
    }

// mutation methods are unsupported
    public boolean add(T o) {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean addAll(Collection<? extends T> c) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }
}
