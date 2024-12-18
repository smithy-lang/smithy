/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.node;

import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.IdRefTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Validates that the value contained in a string shape is a valid shape ID
 * and that the shape ID targets a shape that is in the set of shapes
 * matching the selector.
 */
@SmithyInternalApi
final class IdRefPlugin extends MemberAndShapeTraitPlugin<StringShape, StringNode, IdRefTrait> {

    IdRefPlugin() {
        super(StringShape.class, StringNode.class, IdRefTrait.class);
    }

    @Override
    protected void check(Shape shape, IdRefTrait trait, StringNode node, Context context, Emitter emitter) {
        try {
            ShapeId target = node.expectShapeId();
            Shape resolved = context.model().getShape(target).orElse(null);

            if (resolved == null) {
                if (trait.failWhenMissing()) {
                    failWhenNoMatch(node,
                            trait,
                            emitter,
                            String.format(
                                    "Shape ID `%s` was not found in the model",
                                    target));
                }
            } else {
                if (!matchesSelector(trait, resolved, context)) {
                    failWhenNoMatch(node,
                            trait,
                            emitter,
                            String.format(
                                    "Shape ID `%s` does not match selector `%s`",
                                    resolved.getId(),
                                    trait.getSelector()));
                }
            }
        } catch (SourceException e) {
            emitter.accept(node, Severity.ERROR, e.getMessageWithoutLocation());
        }
    }

    private boolean matchesSelector(IdRefTrait trait, Shape needle, Context context) {
        if (trait.getSelector().toString().equals("*")) {
            return true;
        } else {
            return context.select(trait.getSelector()).contains(needle);
        }
    }

    private void failWhenNoMatch(FromSourceLocation location, IdRefTrait trait, Emitter emitter, String message) {
        emitter.accept(location, Severity.ERROR, trait.getErrorMessage().orElse(message));
    }
}
