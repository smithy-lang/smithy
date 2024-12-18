/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.node;

import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.PatternTrait;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.Severity;

/**
 * Validates the pattern trait on string shapes or members that target them.
 */
final class PatternTraitPlugin extends MemberAndShapeTraitPlugin<StringShape, StringNode, PatternTrait> {

    PatternTraitPlugin() {
        super(StringShape.class, StringNode.class, PatternTrait.class);
    }

    @Override
    protected void check(Shape shape, PatternTrait trait, StringNode node, Context context, Emitter emitter) {
        if (!trait.getPattern().matcher(node.getValue()).find()) {
            emitter.accept(node,
                    getSeverity(context),
                    String.format(
                            "String value provided for `%s` must match regular expression: %s",
                            shape.getId(),
                            trait.getPattern().pattern()));
        }
    }

    private Severity getSeverity(Context context) {
        return context.hasFeature(NodeValidationVisitor.Feature.ALLOW_CONSTRAINT_ERRORS)
                ? Severity.WARNING
                : Severity.ERROR;
    }
}
