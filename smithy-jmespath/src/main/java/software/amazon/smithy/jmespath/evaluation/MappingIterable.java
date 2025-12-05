/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.Iterator;
import java.util.function.Function;

public class MappingIterable<T, R> implements Iterable<R> {

    private final Iterable<T> inner;
    private final Function<? super T, ? extends R> mapping;

    public MappingIterable(Function<T, R> mapping, Iterable<T> inner) {
        this.inner = inner;
        this.mapping = mapping;
    }

    @Override
    public Iterator<R> iterator() {
        return new WrappingIterator(inner.iterator());
    }

    private class WrappingIterator implements Iterator<R> {

        private final Iterator<? extends T> inner;

        private WrappingIterator(Iterator<? extends T> inner) {
            this.inner = inner;
        }

        @Override
        public boolean hasNext() {
            return inner.hasNext();
        }

        @Override
        public R next() {
            return mapping.apply(inner.next());
        }
    }
}
