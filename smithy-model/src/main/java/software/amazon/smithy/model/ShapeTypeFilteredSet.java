/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Provides a view of a set returned by the getXwithTrait methods so that only
 * shapes of a specific type are treated as present within the set.
 *
 * @param <T> Type of shape to project out of the Set.
 */
final class ShapeTypeFilteredSet<T extends Shape> extends AbstractSet<T> {

    private final Set<Shape> shapes;
    private final Class<T> shapeType;

    // -1 means that the size is yet to be computed and cached.
    private volatile int size = -1;

    ShapeTypeFilteredSet(Set<Shape> shapes, Class<T> shapeType) {
        this.shapes = shapes;
        this.shapeType = shapeType;
    }

    @Override
    public boolean contains(Object o) {
        return o.getClass() == shapeType && super.contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        return new FilteredIterator<>(shapes.iterator(), shapeType);
    }

    @Override
    public int size() {
        // Computing the size of the set is O(N) because we need to iterate
        // over every value and test if it's of the right type. The size is
        // cached to avoid doing this repeatedly.
        int result = size;

        if (result == -1) {
            result = 0;
            for (Shape shape : shapes) {
                if (shapeType == shape.getClass()) {
                    result++;
                }
            }
            size = result;
        }

        return result;
    }

    private static final class FilteredIterator<T extends Shape> implements Iterator<T> {
        private final Iterator<Shape> iterator;
        private final Class<T> shapeType;
        private T next;

        FilteredIterator(Iterator<Shape> iterator, Class<T> shapeType) {
            this.iterator = iterator;
            this.shapeType = shapeType;

            // Always compute the first element in the iterator.
            next = computeNext();
        }

        @SuppressWarnings("unchecked")
        private T computeNext() {
            // Filter out shapes of other types.
            while (iterator.hasNext()) {
                Shape nextCandidate = iterator.next();
                if (shapeType == nextCandidate.getClass()) {
                    return (T) nextCandidate;
                }
            }
            return null;
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public T next() {
            if (next == null) {
                throw new NoSuchElementException("No more shapes in iterator");
            }

            T result = next;
            next = computeNext();

            return result;
        }
    }
}
