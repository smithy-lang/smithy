/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.shapes;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.SmithyBuilder;
import software.amazon.smithy.model.ToSmithyBuilder;

/**
 * Provides an index of {@link Shape}s in a Smithy model by {@link ShapeId}.
 *
 * <p>The {@code ShapeIndex} represents a Smithy model in a flat graph-like
 * index. Shapes reference other shapes by shape ID, and these shape IDs are
 * used then matched with shapes in a {@code ShapeIndex}.
 *
 * Using this kind of graph-like structure rather than a something more like
 * a tree allows for a fast iteration over all shapes in a model, allows for
 * shapes to reference other shapes without requiring cyclic references, and
 * maps cleanly to the mental model of a Smithy model definition (that is,
 * shape IDs are used to connect shapes to other shapes, allow traits to be
 * applied to shapes, and are used to suppress validation events). However,
 * this does come with the trade-off that the {@code ShapeIndex} may be an an
 * invalid state when it is accessed. As such, a {@code ShapeIndex} should be
 * thoroughly validated before it is utilized.
 */
public final class ShapeIndex implements ToSmithyBuilder<ShapeIndex> {
    /**
     * The default number of shapes that must be present to use a cache
     * when extracting streams of shapes of a specific type. With smaller
     * shape maps, it's more efficient to just recompute the result for
     * each invocation.
     */
    private static final int GROUP_CACHE_THRESHOLD = 50;

    /** A map of shape ID to shapes that backs the shape map. */
    private final Map<ShapeId, Shape> shapeMap;

    /** Responsible for providing streams of shapes of a specific type. */
    private final ShapeGrouper shapeGrouper;

    /** Lazily computed hash code of the shape index. */
    private int hash;

    private ShapeIndex(Builder builder) {
        shapeMap = Map.copyOf(builder.shapeMap);

        // Only cache the results of querying shapes by type if the number
        // of shapes in the index exceeds the threshold.
        shapeGrouper = shapeMap.size() >= GROUP_CACHE_THRESHOLD
                ? new CachedShapeGrouper()
                : ShapeIndex::passThroughGrouper;
    }

    /**
     * Handles the caching of shapes of a specific type.
     */
    @FunctionalInterface
    private interface ShapeGrouper {
        <T extends Shape> Stream<T> shapes(Stream<Shape> shapes, Class<T> shapeType);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Shape> Stream<T> passThroughGrouper(Stream<Shape> shapes, Class<T> shapeType) {
        return (Stream<T>) shapes.filter(value -> value.getClass() == shapeType);
    }

    private static final class CachedShapeGrouper implements ShapeGrouper {
        private final Map<Class<? extends Shape>, List<Shape>> grouped = new ConcurrentHashMap<>();

        @Override
        @SuppressWarnings("unchecked")
        public <T extends Shape> Stream<T> shapes(Stream<Shape> shapes, Class<T> shapeType) {
            return (Stream<T>) grouped
                    .computeIfAbsent(shapeType, t -> passThroughGrouper(shapes, shapeType)
                            .collect(Collectors.toList()))
                    .stream();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder().addShapes(this);
    }

    /**
     * Attempts to retrieve a {@link Shape} by {@link ShapeId}.
     *
     * @param id Shape to retrieve by ID.
     * @return Returns the optional shape.
     */
    public Optional<Shape> getShape(ShapeId id) {
        return Optional.ofNullable(shapeMap.get(id));
    }

    /**
     * Gets a stream of {@link Shape}s in the index.
     *
     * @return Returns a stream of shapes.
     */
    public Stream<Shape> shapes() {
        return shapeMap.values().stream();
    }

    /**
     * Gets a stream of shapes in the index of a specific type {@code T}.
     *
     * <p>The provided shapeType class must exactly match the class of a
     * shape in the shape index in order to be returned from this method;
     * that is, the provided class must be a concrete subclass of
     * {@link Shape} and not an abstract class like {@link NumberShape}.
     *
     * @param shapeType Shape type {@code T} to retrieve.
     * @param <T> Shape type to stream from the index.
     * @return A stream of shapes of {@code T} matching {@code shapeType}.
     */
    public <T extends Shape> Stream<T> shapes(Class<T> shapeType) {
        return shapeGrouper.shapes(shapes(), shapeType);
    }

    /**
     * Converts the ShapeIndex to an immutable Set of shapes.
     *
     * @return Returns an unmodifiable set of Shapes in the index.
     */
    public Set<Shape> toSet() {
        return new AbstractSet<Shape>() {
            @Override
            public int size() {
                return shapeMap.size();
            }

            @Override
            public boolean contains(Object o) {
                return o instanceof Shape && shapeMap.containsKey(((Shape) o).getId());
            }

            @Override
            public Iterator<Shape> iterator() {
                return shapeMap.values().iterator();
            }
        };
    }

    @Override
    public int hashCode() {
        int result = hash;
        if (result == 0) {
            result = shapeMap.keySet().hashCode();
            hash = result;
        }
        return result;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ShapeIndex && shapeMap.equals(((ShapeIndex) other).shapeMap);
    }

    /**
     * Builder used to construct a ShapeIndex.
     */
    public static final class Builder implements SmithyBuilder<ShapeIndex> {
        /** All shapes to add to the index. */
        private final Map<ShapeId, Shape> shapeMap = new HashMap<>();

        private Builder() {}

        @Override
        public ShapeIndex build() {
            return new ShapeIndex(this);
        }

        /**
         * Add a shape to the builder.
         *
         * @param shape Shape to add.
         * @return Returns the builder.
         */
        public Builder addShape(Shape shape) {
            shapeMap.put(shape.getId(), shape);
            return this;
        }

        /**
         * Adds the shapes of a ShapeIndex to the builder.
         *
         * @param shapeIndex Shape index to add shapes from.
         * @return Returns the builder.
         */
        public Builder addShapes(ShapeIndex shapeIndex) {
            shapeIndex.shapes().forEach(this::addShape);
            return this;
        }

        /**
         * Adds a collection of shapes to the builder.
         *
         * @param shapes Collection of Shapes to add.
         * @return Returns the builder.
         */
        public Builder addShapes(Collection<Shape> shapes) {
            shapes.forEach(this::addShape);
            return this;
        }

        /**
         * Adds a variadic list of shapes.
         *
         * @param shapes Shapes to add.
         * @return Returns the builder.
         */
        public Builder addShapes(Shape... shapes) {
            for (Shape shape : shapes) {
                addShape(shape);
            }
            return this;
        }

        /**
         * Removes a shape from the builder by ID.
         *
         * @param shapeId Shape to remove.
         * @return Returns the builder.
         */
        public Builder removeShape(ShapeId shapeId) {
            shapeMap.remove(shapeId);
            return this;
        }
    }
}
