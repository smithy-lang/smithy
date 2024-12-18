/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.node;

import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates the length trait on map shapes or members that target them.
 */
@SmithyInternalApi
final class MapLengthPlugin extends MemberAndShapeTraitPlugin<MapShape, ObjectNode, LengthTrait> {

    MapLengthPlugin() {
        super(MapShape.class, ObjectNode.class, LengthTrait.class);
    }

    @Override
    protected void check(Shape shape, LengthTrait trait, ObjectNode node, Context context, Emitter emitter) {
        trait.getMin().ifPresent(min -> {
            if (node.size() < min) {
                emitter.accept(node,
                        getSeverity(context),
                        String.format(
                                "Value provided for `%s` must have at least %d entries, but the provided value only "
                                        + "has %d entries",
                                shape.getId(),
                                min,
                                node.size()));
            }
        });

        trait.getMax().ifPresent(max -> {
            if (node.size() > max) {
                emitter.accept(node,
                        getSeverity(context),
                        String.format(
                                "Value provided for `%s` must have no more than %d entries, but the provided value "
                                        + "has %d entries",
                                shape.getId(),
                                max,
                                node.size()));
            }
        });
    }

    private Severity getSeverity(Context context) {
        return context.hasFeature(NodeValidationVisitor.Feature.ALLOW_CONSTRAINT_ERRORS)
                ? Severity.WARNING
                : Severity.ERROR;
    }
}
