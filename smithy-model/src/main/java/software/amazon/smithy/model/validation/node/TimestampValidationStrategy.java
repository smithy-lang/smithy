/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.node;

import java.util.function.BiPredicate;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ShapeTypeFilter;

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
        public BiPredicate<Model, Shape> shapeMatcher() {
            return new TimestampFormatPlugin().shapeMatcher();
        }

        @Override
        public void applyMatching(Shape shape, Node value, Context context, Emitter emitter) {
            new TimestampFormatPlugin().applyMatching(shape, value, context, emitter);
        }
    },

    /**
     * Requires that the value provided for all timestamp shapes is a
     * unix timestamp.
     */
    EPOCH_SECONDS {
        @Override
        public BiPredicate<Model, Shape> shapeMatcher() {
            return new ShapeTypeFilter(ShapeType.TIMESTAMP);
        }

        @Override
        public void applyMatching(Shape shape, Node value, Context context, Emitter emitter) {
            if (!value.isNumberNode()) {
                emitter.accept(shape,
                        "Invalid " + value.getType() + " value provided for timestamp, `"
                                + shape.getId() + "`. Expected a number that contains epoch "
                                + "seconds with optional millisecond precision");
            }
        }
    };
}
