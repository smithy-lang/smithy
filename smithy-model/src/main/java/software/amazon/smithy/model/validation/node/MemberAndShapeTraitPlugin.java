/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.node;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.Trait;

abstract class MemberAndShapeTraitPlugin<S extends Shape, N extends Node, T extends Trait>
        implements NodeValidatorPlugin {

    private final Class<S> targetShapeClass;
    private final Class<N> nodeClass;
    private final Class<T> traitClass;

    MemberAndShapeTraitPlugin(Class<S> targetShapeClass, Class<N> nodeClass, Class<T> traitClass) {
        this.targetShapeClass = targetShapeClass;
        this.nodeClass = nodeClass;
        this.traitClass = traitClass;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void apply(Shape shape, Node value, Context context, Emitter emitter) {
        if (nodeClass.isInstance(value)
                && shape.getTrait(traitClass).isPresent()
                && isMatchingShape(shape, context.model())) {
            check(shape, shape.getTrait(traitClass).get(), (N) value, context, emitter);
        }
    }

    private boolean isMatchingShape(Shape shape, Model model) {
        // Is the shape the expected shape type?
        if (targetShapeClass.isInstance(shape)) {
            return true;
        }

        // Is the targeted member an instance of the expected shape?
        return shape.asMemberShape()
                .flatMap(member -> model.getShape(member.getTarget()))
                .filter(targetShapeClass::isInstance)
                .isPresent();
    }

    protected abstract void check(Shape shape, T trait, N value, Context context, Emitter emitter);
}
