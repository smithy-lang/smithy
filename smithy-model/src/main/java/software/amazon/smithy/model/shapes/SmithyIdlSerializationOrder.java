/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import java.util.Comparator;
import java.util.Map;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.traits.Trait;

/**
 * Configures how shapes, traits, and metadata are ordered when serializing a model
 * with {@link SmithyIdlModelSerializer}.
 *
 * <p>The built-in orderings are available as constants on {@link SmithyIdlComponentOrder}.
 * Implement this interface directly to provide custom ordering logic.
 *
 * <p>Only {@link #shapeComparator()} is required. The default implementations of
 * {@link #traitComparator()} and {@link #metadataComparator()} sort alphabetically,
 * which is suitable for most use cases.
 *
 * @see SmithyIdlComponentOrder
 * @see SmithyIdlModelSerializer.Builder#componentOrder(SmithyIdlSerializationOrder)
 */
public interface SmithyIdlSerializationOrder {

    /**
     * A shape comparator that sorts alphabetically by shape ID.
     */
    Comparator<Shape> ALPHABETICAL = Comparator.comparing(Shape::toShapeId);

    /**
     * Returns a comparator used to sort shapes within a namespace file.
     *
     * @return Comparator for ordering shapes.
     */
    Comparator<Shape> shapeComparator();

    /**
     * Returns a comparator used to sort traits applied to a shape.
     *
     * <p>The default implementation sorts traits alphabetically by their shape ID. Custom
     * implementations that use source-location-based ordering should consider overriding
     * this method to also sort traits by source location for consistency.
     *
     * @return Comparator for ordering traits.
     */
    default Comparator<Trait> traitComparator() {
        return Comparator.comparing(Trait::toShapeId);
    }

    /**
     * Returns a comparator used to sort metadata entries.
     *
     * <p>The default implementation sorts metadata alphabetically by key.
     *
     * @return Comparator for ordering metadata entries.
     */
    default Comparator<Map.Entry<String, Node>> metadataComparator() {
        return Map.Entry.comparingByKey();
    }
}
