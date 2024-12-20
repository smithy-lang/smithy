/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.utils.MapUtils;

/**
 * Defines how shapes, traits, and metadata are sorted when serializing a model with {@link SmithyIdlModelSerializer}.
 */
public enum SmithyIdlComponentOrder {
    /**
     * Sort shapes, traits, and metadata alphabetically. Member order, however, is not sorted.
     */
    ALPHA_NUMERIC,

    /**
     * Sort shapes, traits, and metadata by source location, persisting their original placement when parsed.
     */
    SOURCE_LOCATION,

    /**
     * Reorganizes shapes based on a preferred ordering of shapes, and alphanumeric traits and metadata.
     *
     * <p>Shapes are ordered as follows:
     *
     * <ul>
     *     <li>Trait definitions</li>
     *     <li>Services</li>
     *     <li>Resources</li>
     *     <li>Operations</li>
     *     <li>Structures</li>
     *     <li>Unions</li>
     *     <li>Lists</li>
     *     <li>Maps</li>
     *     <li>Finally, alphabetically by shape type.</li>
     * </ul>
     */
    PREFERRED;

    Comparator<Shape> shapeComparator() {
        return this == PREFERRED ? new PreferredShapeComparator() : toShapeIdComparator();
    }

    <T extends FromSourceLocation & ToShapeId> Comparator<T> toShapeIdComparator() {
        switch (this) {
            case PREFERRED:
            case ALPHA_NUMERIC:
                return Comparator.comparing(ToShapeId::toShapeId);
            case SOURCE_LOCATION:
            default:
                return new SourceComparator<>();
        }
    }

    Comparator<Map.Entry<String, Node>> metadataComparator() {
        switch (this) {
            case ALPHA_NUMERIC:
            case PREFERRED:
                return Map.Entry.comparingByKey();
            case SOURCE_LOCATION:
            default:
                return new MetadataComparator();
        }
    }

    private static final class SourceComparator<T extends FromSourceLocation & ToShapeId>
            implements Comparator<T>, Serializable {
        @Override
        public int compare(T a, T b) {
            SourceLocation left = a.getSourceLocation();
            SourceLocation right = b.getSourceLocation();
            int comparison = left.compareTo(right);
            return comparison != 0 ? comparison : a.toShapeId().compareTo(b.toShapeId());
        }
    }

    private static final class MetadataComparator implements Comparator<Map.Entry<String, Node>>, Serializable {
        @Override
        public int compare(Map.Entry<String, Node> a, Map.Entry<String, Node> b) {
            SourceLocation left = a.getValue().getSourceLocation();
            SourceLocation right = b.getValue().getSourceLocation();
            int comparison = left.compareTo(right);
            return comparison != 0 ? comparison : a.getKey().compareTo(b.getKey());
        }
    }

    /**
     * Comparator used to sort shapes.
     */
    private static final class PreferredShapeComparator implements Comparator<Shape>, Serializable {
        private static final Map<ShapeType, Integer> PRIORITY = MapUtils.of(
                ShapeType.SERVICE,
                0,
                ShapeType.RESOURCE,
                1,
                ShapeType.OPERATION,
                2,
                ShapeType.STRUCTURE,
                3,
                ShapeType.UNION,
                4,
                ShapeType.LIST,
                5,
                ShapeType.SET,
                6,
                ShapeType.MAP,
                7);

        @Override
        public int compare(Shape s1, Shape s2) {
            // Traits go first
            if (s1.hasTrait(TraitDefinition.class) || s2.hasTrait(TraitDefinition.class)) {
                if (!s1.hasTrait(TraitDefinition.class)) {
                    return 1;
                }
                if (!s2.hasTrait(TraitDefinition.class)) {
                    return -1;
                }
                // The other sorting rules don't matter for traits.
                return s1.compareTo(s2);
            }
            // If the shapes are the same type, just compare their shape ids.
            if (s1.getType().equals(s2.getType())) {
                return s1.compareTo(s2);
            }
            // If one shape is prioritized, compare by priority.
            if (PRIORITY.containsKey(s1.getType()) || PRIORITY.containsKey(s2.getType())) {
                // If only one shape is prioritized, that shape is "greater".
                if (!PRIORITY.containsKey(s1.getType())) {
                    return 1;
                }
                if (!PRIORITY.containsKey(s2.getType())) {
                    return -1;
                }
                return PRIORITY.get(s1.getType()) - PRIORITY.get(s2.getType());
            }
            return s1.compareTo(s2);
        }
    }
}
