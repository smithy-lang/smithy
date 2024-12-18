/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.node;

import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.Severity;

/**
 * Validates the length trait on string shapes or members that target them.
 */
final class StringLengthPlugin extends MemberAndShapeTraitPlugin<StringShape, StringNode, LengthTrait> {

    StringLengthPlugin() {
        super(StringShape.class, StringNode.class, LengthTrait.class);
    }

    @Override
    protected void check(Shape shape, LengthTrait trait, StringNode node, Context context, Emitter emitter) {
        trait.getMin().ifPresent(min -> {
            if (node.getValue().length() < min) {
                emitter.accept(node,
                        getSeverity(context),
                        String.format(
                                "String value provided for `%s` must be >= %d characters, but the provided value is "
                                        + "only %d characters.",
                                shape.getId(),
                                min,
                                node.getValue().length()));
            }
        });

        trait.getMax().ifPresent(max -> {
            if (node.getValue().length() > max) {
                emitter.accept(node,
                        getSeverity(context),
                        String.format(
                                "String value provided for `%s` must be <= %d characters, but the provided value is "
                                        + "%d characters.",
                                shape.getId(),
                                max,
                                node.getValue().length()));
            }
        });
    }

    private Severity getSeverity(Context context) {
        return context.hasFeature(NodeValidationVisitor.Feature.ALLOW_CONSTRAINT_ERRORS)
                ? Severity.WARNING
                : Severity.ERROR;
    }
}
