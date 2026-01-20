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
import software.amazon.smithy.model.traits.Trait;

public abstract class MemberAndShapeTraitPlugin<N extends Node, T extends Trait>
        implements NodeValidatorPlugin {

    private final EnumSet<ShapeType> shapeTypes;
    private final Class<N> nodeClass;
    private final Class<T> traitClass;

    public MemberAndShapeTraitPlugin(EnumSet<ShapeType> shapeTypes, Class<N> nodeClass, Class<T> traitClass) {
        this.shapeTypes = shapeTypes;
        this.nodeClass = nodeClass;
        this.traitClass = traitClass;
    }

    @Override
    public BiPredicate<Model, Shape> shapeMatcher() {
        return new ShapeTypeFilter(shapeTypes);
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
