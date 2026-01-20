/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.node;

import java.util.EnumSet;
import java.util.function.BiPredicate;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ShapeTypeFilter;

abstract class FilteredPlugin<S extends Shape, N extends Node> implements NodeValidatorPlugin {
    private final EnumSet<ShapeType> shapeTypes;
    private final Class<S> shapeClass;
    private final Class<N> nodeClass;

    FilteredPlugin(EnumSet<ShapeType> shapeTypes, Class<S> shapeClass, Class<N> nodeClass) {
        for (ShapeType shapeType : shapeTypes) {
            if (!shapeClass.isAssignableFrom(shapeType.getShapeClass())) {
                throw new IllegalArgumentException();
            }
        }
        this.shapeTypes = shapeTypes;
        this.shapeClass = shapeClass;
        this.nodeClass = nodeClass;
    }

    @Override
    public BiPredicate<Model, Shape> shapeMatcher() {
        // Only direct shapes, not member shapes pointing to them
        return new ShapeTypeFilter(shapeTypes, EnumSet.noneOf(ShapeType.class));
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void applyMatching(Shape shape, Node value, Context context, Emitter emitter) {
        if (nodeClass.isInstance(value)) {
            check((S) shape, (N) value, context, emitter);
        }
    }

    abstract void check(S shape, N node, Context context, Emitter emitter);
}
