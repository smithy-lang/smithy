/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.RequestCompressionTrait;
import software.amazon.smithy.model.traits.RequiresLengthTrait;
import software.amazon.smithy.model.traits.StreamingTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * Validates the requestCompression trait.
 *
 * - Validates that the operation input has no member with both streaming and
 *   requiresLength traits applied
 * - Validate encodings has at least one compression algorithm
 * - Validate encodings are all supported compression algorithms
 */
public final class RequestCompressionTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (OperationShape shape : model.getOperationShapesWithTrait(RequestCompressionTrait.class)) {
            validateOperationInput(model, shape, events);
            validateEncodings(model, shape, events);
        }
        return events;
    }

    private void validateOperationInput(Model model, OperationShape operationShape, List<ValidationEvent> events) {
        // Validate operation input has no member with both streaming and requiresLength
        // traits applied
        if (operationShape.getInputShape().equals(ShapeId.from("smithy.api#Unit"))) {
            events.add(error(operationShape,
                    "The `requestCompression` trait can only be applied to operations which have an "
                            + "input shape defined"));
            return;
        }
        StructureShape inputShape = model.expectShape(operationShape.getInputShape(), StructureShape.class);
        for (MemberShape memberShape : inputShape.members()) {
            Shape targetShape = model.expectShape(memberShape.getTarget());
            if (targetShape.hasTrait(StreamingTrait.class) && targetShape.hasTrait(RequiresLengthTrait.class)) {
                events.add(error(operationShape,
                        String.format(
                                "The `requestCompression` trait can only be applied to operations which do not have "
                                        + "input members that target shapes with both the `streaming` and `requiresLength` "
                                        + "traits applied, but found member `%s` targeting `%s`",
                                memberShape.getId(),
                                targetShape.getId())));
            }
        }
    }

    private void validateEncodings(Model model, OperationShape operationShape, List<ValidationEvent> events) {
        // Validate encodings have at least one compression algorithm
        RequestCompressionTrait trait = operationShape.expectTrait(RequestCompressionTrait.class);
        if (trait.getEncodings().isEmpty()) {
            events.add(error(operationShape,
                    trait,
                    String.format(
                            "There must be at least one compression algorithm in `encodings` for the "
                                    + "`requestCompression` trait applied to `%s`",
                            operationShape.getId())));
            return;
        }
        // Validate encodings are all supported compression algorithms
        List<String> invalidEncodings = new ArrayList<>();
        for (String encoding : trait.getEncodings()) {
            if (!isValidEncoding(encoding)) {
                invalidEncodings.add(encoding);
            }
        }
        if (!invalidEncodings.isEmpty()) {
            events.add(error(operationShape,
                    trait,
                    String.format(
                            "Invalid compression algorithm%s found in `requestCompression` trait applied to `%s`: %s",
                            invalidEncodings.size() == 1 ? "" : "s",
                            operationShape.getId(),
                            ValidationUtils.tickedList(invalidEncodings))));
        }
    }

    private static boolean isValidEncoding(String encoding) {
        return RequestCompressionTrait.SUPPORTED_COMPRESSION_ALGORITHMS.contains(encoding.toLowerCase());
    }
}
