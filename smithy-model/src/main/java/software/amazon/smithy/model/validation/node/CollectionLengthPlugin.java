/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.node;

import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ShapeTypeFilter;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates the length trait on both list and set shapes or members that
 * target them.
 */
@SmithyInternalApi
final class CollectionLengthPlugin extends MemberAndShapeTraitPlugin<ArrayNode, LengthTrait> {

    private static final ShapeTypeFilter SHAPE_TYPE_FILTER = new ShapeTypeFilter(ShapeType.COLLECTION_TYPES);

    CollectionLengthPlugin() {
        super(ArrayNode.class, LengthTrait.class);
    }

    @Override
    public ShapeTypeFilter shapeTypeFilter() {
        return SHAPE_TYPE_FILTER;
    }

    @Override
    protected void check(Shape shape, LengthTrait trait, ArrayNode node, Context context, Emitter emitter) {
        trait.getMin().ifPresent(min -> {
            if (node.size() < min) {
                emitter.accept(node,
                        String.format(
                                "Value provided for `%s` must have at least %d elements, but the provided value only "
                                        + "has %d elements",
                                shape.getId(),
                                min,
                                node.size()));
            }
        });

        trait.getMax().ifPresent(max -> {
            if (node.size() > max) {
                emitter.accept(node,
                        String.format(
                                "Value provided for `%s` must have no more than %d elements, but the provided value "
                                        + "has %d elements",
                                shape.getId(),
                                max,
                                node.size()));
            }
        });
    }
}
