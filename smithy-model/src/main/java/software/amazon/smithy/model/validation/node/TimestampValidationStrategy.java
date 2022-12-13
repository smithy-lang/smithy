/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
                emitter.accept(shape, "Invalid " + value.getType() + " value provided for timestamp, `"
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
