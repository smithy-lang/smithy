/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.node;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Defines how timestamps are validated.
 */
public enum TimestampValidationStrategy implements NodeValidatorPlugin {

    /**
     * Validates timestamps by requiring that the value uses matches the
     * resolved timestamp format, or is a unix timestamp or integer in the
     * case that a member or shape does not have a {@code timestampFormat}
     * trait.
     */
    FORMAT {
        @Override
        public void apply(Shape shape, Node value, Context context, Emitter emitter) {
            new TimestampFormatPlugin().apply(shape, value, context, emitter);
        }
    },

    /**
     * Requires that the value provided for all timestamp shapes is a
     * unix timestamp.
     */
    EPOCH_SECONDS {
        @Override
        public void apply(Shape shape, Node value, Context context, Emitter emitter) {
            if (isTimestampMember(context.model(), shape) && !value.isNumberNode()) {
                emitter.accept(shape,
                        "Invalid " + value.getType() + " value provided for timestamp, `"
                                + shape.getId() + "`. Expected a number that contains epoch "
                                + "seconds with optional millisecond precision");
            }
        }
    };

    private static boolean isTimestampMember(Model model, Shape shape) {
        return shape.asMemberShape()
                .map(MemberShape::getTarget)
                .flatMap(model::getShape)
                .filter(Shape::isTimestampShape)
                .isPresent();
    }
}
