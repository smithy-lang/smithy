/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.node;

import java.util.EnumSet;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ShapeTypeFilter;

abstract class FilteredPlugin<S extends Shape, N extends Node> implements NodeValidatorPlugin {
    // The set of ShapeTypes whose class is a subclass of the S type
    private final ShapeTypeFilter shapeTypeFilter;
    private final Class<S> shapeClass;
    private final Class<N> nodeClass;

    FilteredPlugin(Class<S> shapeClass, Class<N> nodeClass) {
        this.shapeClass = shapeClass;
        this.nodeClass = nodeClass;

        EnumSet<ShapeType> shapeTypes = EnumSet.noneOf(ShapeType.class);
        for (ShapeType shapeType : ShapeType.values()) {
            if (shapeClass.isAssignableFrom(shapeType.getShapeClass())) {
                shapeTypes.add(shapeType);
            }
        }
        // Only applies to direct shapes, not member shapes pointing to them,
        // since the plugin only applies to subclasses of S.
        shapeTypeFilter = new ShapeTypeFilter(shapeTypes, EnumSet.noneOf(ShapeType.class));
    }

    @Override
    public ShapeTypeFilter shapeTypeFilter() {
        return shapeTypeFilter;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void applyMatching(Shape shape, Node value, Context context, Emitter emitter) {
        if (nodeClass.isInstance(value)) {
            // The cast to (S) is safe because of the ShapeType guard
            check((S) shape, (N) value, context, emitter);
        }
    }

    abstract void check(S shape, N node, Context context, Emitter emitter);
}
