/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.node;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.Trait;

public abstract class MemberAndShapeTraitPlugin<N extends Node, T extends Trait>
        implements NodeValidatorPlugin {

    private final Class<N> nodeClass;
    private final Class<T> traitClass;

    public MemberAndShapeTraitPlugin(Class<N> nodeClass, Class<T> traitClass) {
        this.nodeClass = nodeClass;
        this.traitClass = traitClass;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void applyMatching(Shape shape, Node value, Context context, Emitter emitter) {
        if (nodeClass.isInstance(value)
                && shape.getTrait(traitClass).isPresent()) {
            check(shape, shape.getTrait(traitClass).get(), (N) value, context, emitter);
        }
    }

    protected abstract void check(Shape shape, T trait, N value, Context context, Emitter emitter);
}
