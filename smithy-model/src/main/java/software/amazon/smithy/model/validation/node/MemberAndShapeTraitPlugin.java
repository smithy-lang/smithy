/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.node;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.Trait;

/**
 * An abstract base class for plugins that apply only to shapes with a given applied trait.
 *
 * @param <N> The type of the subclass of {@link Node} this plugin applies to.
 * @param <T> The type of the trait class this plugin applies to.
 */
public abstract class MemberAndShapeTraitPlugin<N extends Node, T extends Trait>
        implements NodeValidatorPlugin {

    private final Class<N> nodeClass;
    private final Class<T> traitClass;

    /**
     * @param nodeClass The subclass of {@link Node} this plugin applies to.
     * @param traitClass The trait class this plugin applies to.
     */
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

    /**
     * Validates the given node value.
     * <p>
     * Implementors can assume the given trait is applied to the given shape.
     *
     * @param shape The shape the given value is for.
     * @param trait The applied trait on the given shape.
     * @param value The {@link Node} value to validate.
     * @param context The plugin validation context.
     * @param emitter The emitter to emit any validation events that occur.
     */
    protected abstract void check(Shape shape, T trait, N value, Context context, Emitter emitter);
}
