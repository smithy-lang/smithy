/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.node;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;

abstract class FilteredPlugin<S extends Shape, N extends Node> implements NodeValidatorPlugin {
    private final Class<S> shapeClass;
    private final Class<N> nodeClass;

    FilteredPlugin(Class<S> shapeClass, Class<N> nodeClass) {
        this.shapeClass = shapeClass;
        this.nodeClass = nodeClass;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void apply(Shape shape, Node value, Context context, Emitter emitter) {
        if (shapeClass.isInstance(shape) && nodeClass.isInstance(value)) {
            check((S) shape, (N) value, context, emitter);
        }
    }

    abstract void check(S shape, N node, Context context, Emitter emitter);
}
