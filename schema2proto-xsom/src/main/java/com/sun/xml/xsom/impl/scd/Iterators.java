/*
 * Copyright (c) 1997, 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package com.sun.xml.xsom.impl.scd;

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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Collections;

/**
 * Various convenient {@link Iterator} implementations.
 * @author Kohsuke Kawaguchi
 */
public class Iterators {

    static abstract class ReadOnly<T> implements Iterator<T> {
        public final void remove() {
            throw new UnsupportedOperationException();
        }
    }

    // we need to run on JDK 1.4
    private static final Iterator EMPTY = Collections.EMPTY_LIST.iterator();

    public static <T> Iterator<T> empty() {
        return EMPTY;
    }

    public static <T> Iterator<T> singleton(T value) {
        return new Singleton<T>(value);
    }

    /**
     * {@link Iterator} that returns a single (or no) value.
     */
    static final class Singleton<T> extends ReadOnly<T> {
        private T next;

        Singleton(T next) {
            this.next = next;
        }

        public boolean hasNext() {
            return next!=null;
        }

        public T next() {
            T r = next;
            next = null;
            return r;
        }
    }

    /**
     * {@link Iterator} that wraps another {@link Iterator} and changes its type.
     */
    public static abstract class Adapter<T,U> extends ReadOnly<T> {
        private final Iterator<? extends U> core;

        public Adapter(Iterator<? extends U> core) {
            this.core = core;
        }

        public boolean hasNext() {
            return core.hasNext();
        }

        public T next() {
            return filter(core.next());
        }

        protected abstract T filter(U u);
    }

    /**
     * For each U, apply {@code U->Iterator<T>} function and then iterate all
     * the resulting T.
     */
    public static abstract class Map<T,U> extends ReadOnly<T> {
        private final Iterator<? extends U> core;

        private Iterator<? extends T> current;

        protected Map(Iterator<? extends U> core) {
            this.core = core;
        }

        public boolean hasNext() {
            while(current==null || !current.hasNext()) {
                if(!core.hasNext())
                    return false;   // nothing more to enumerate
                current = apply(core.next());
            }
            return true;
        }

        public T next() {
            return current.next();
        }

        protected abstract Iterator<? extends T> apply(U u);
    }

    /**
     * Filter out objects from another iterator.
     */
    public static abstract class Filter<T> extends ReadOnly<T> {
        private final Iterator<? extends T> core;
        private T next;

        protected Filter(Iterator<? extends T> core) {
            this.core = core;
        }

        /**
         * Return true to retain the value.
         */
        protected abstract boolean matches(T value);

        public boolean hasNext() {
            while(core.hasNext() && next==null) {
                next = core.next();
                if(!matches(next))
                    next = null;
            }

            return next!=null;
        }

        public T next() {
            if(next==null)      throw new NoSuchElementException();
            T r = next;
            next = null;
            return r;
        }
    }

    /**
     * Only return unique items.
     */
    static final class Unique<T> extends Filter<T> {
        private Set<T> values = new LinkedHashSet<T>();
        public Unique(Iterator<? extends T> core) {
            super(core);
        }

        protected boolean matches(T value) {
            return values.add(value);
        }
    }

    /**
     * Union of two iterators.
     */
    public static final class Union<T> extends ReadOnly<T> {
        private final Iterator<? extends T> first,second;

        public Union(Iterator<? extends T> first, Iterator<? extends T> second) {
            this.first = first;
            this.second = second;
        }

        public boolean hasNext() {
            return first.hasNext() || second.hasNext();
        }

        public T next() {
            if(first.hasNext())     return first.next();
            else                    return second.next();
        }
    }

    /**
     * Array iterator.
     */
    public static final class Array<T> extends ReadOnly<T> {
        private final T[] items;
        private int index=0;
        public Array(T[] items) {
            this.items = items;
        }

        public boolean hasNext() {
            return index<items.length;
        }

        public T next() {
            return items[index++];
        }
    }
}
