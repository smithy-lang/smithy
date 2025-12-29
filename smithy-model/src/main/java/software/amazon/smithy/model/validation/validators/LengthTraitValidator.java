/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ShapeValue;
import software.amazon.smithy.model.knowledge.ShapeValueIndex;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.node.NodeValidatorPlugin;
import software.amazon.smithy.utils.Pair;

public final class LengthTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        ShapeValueIndex shapeValueIndex = ShapeValueIndex.of(model);
        List<ValidationEvent> events = new ArrayList<>();
        for (Shape shape : model.getShapesWithTrait(LengthTrait.class)) {
            events.addAll(validateLengthTrait(shape, shape.expectTrait(LengthTrait.class)));

            // TODO: Could short-circuit and not bother with this if the trait itself is invalid
            for (ShapeValue value : shapeValueIndex.getShapeValues(shape)) {
                validateShapeValue(events, shape, value);
            }
        }

        return events;
    }

    private List<ValidationEvent> validateLengthTrait(Shape shape, LengthTrait trait) {
        List<ValidationEvent> events = new ArrayList<>();
        trait.getMin()
                .filter(min -> min < 0)
                .map(min -> error(shape, trait, "A length trait is applied with a negative `min` value."))
                .ifPresent(events::add);

        trait.getMax()
                .filter(max -> max < 0)
                .map(max -> error(shape, trait, "A length trait is applied with a negative `max` value."))
                .ifPresent(events::add);

        trait.getMin()
                .flatMap(min -> trait.getMax().map(max -> Pair.of(min, max)))
                .filter(pair -> pair.getLeft() > pair.getRight())
                .map(pair -> error(shape,
                        trait,
                        "A length trait is applied with a `min` value greater than "
                                + "its `max` value."))
                .map(events::add);
        return events;
    }

    // Replaces Blob/Collection/Map/StringLengthPlugin
    private void validateShapeValue(List<ValidationEvent> events, Shape shape, ShapeValue value) {
        LengthTrait lengthTrait = shape.expectTrait(LengthTrait.class);

        switch (shape.getType()) {
            case BLOB:
                validateBlobShapeValue(events, shape, lengthTrait, value);
                break;
            case STRING:
                validateStringShapeValue(events, shape, lengthTrait, value);
                break;
            case MAP:
                validateMapShapeValue(events, shape, lengthTrait, value);
                break;
            case LIST:
                validateListShapeValue(events, shape, lengthTrait, value);
                break;
        }
    }

    private void validateMapShapeValue(List<ValidationEvent> events, Shape shape, LengthTrait trait, ShapeValue shapeValue) {
        ObjectNode node = shapeValue.toNode().expectObjectNode();

        trait.getMin().ifPresent(min -> {
            if (node.size() < min) {
                events.add(shapeValue.constraintsEvent(
                        String.format(
                                "Value provided for `%s` must have at least %d entries, but the provided value only "
                                        + "has %d entries",
                                shape.getId(),
                                min,
                                node.size())));
            }
        });

        trait.getMax().ifPresent(max -> {
            if (node.size() > max) {
                events.add(shapeValue.constraintsEvent(
                        String.format(
                                "Value provided for `%s` must have no more than %d entries, but the provided value "
                                        + "has %d entries",
                                shape.getId(),
                                max,
                                node.size())));
            }
        });
    }

    private void validateListShapeValue(List<ValidationEvent> events, Shape shape, LengthTrait trait, ShapeValue shapeValue) {
        ArrayNode node = shapeValue.toNode().expectArrayNode();

        trait.getMin().ifPresent(min -> {
            if (node.size() < min) {
                events.add(shapeValue.constraintsEvent(
                        String.format(
                                "Value provided for `%s` must have at least %d elements, but the provided value only "
                                        + "has %d elements",
                                shape.getId(),
                                min,
                                node.size())));
            }
        });

        trait.getMax().ifPresent(max -> {
            if (node.size() > max) {
                events.add(shapeValue.constraintsEvent(
                        String.format(
                                "Value provided for `%s` must have no more than %d elements, but the provided value "
                                        + "has %d elements",
                                shape.getId(),
                                max,
                                node.size())));
            }
        });
    }

    private void validateBlobShapeValue(List<ValidationEvent> events, Shape shape, LengthTrait trait, ShapeValue shapeValue) {
        byte[] value = shapeValue.toNode().expectStringNode().getValue().getBytes(StandardCharsets.UTF_8);

        if (shapeValue.hasFeature(NodeValidationVisitor.Feature.REQUIRE_BASE_64_BLOB_VALUES)) {
            value = Base64.getDecoder().decode(value);
        }

        int size = value.length;

        trait.getMin().ifPresent(min -> {
            if (size < min) {
                events.add(shapeValue.constraintsEvent(
                        "Value provided for `" + shape.getId()
                                + "` must have at least " + min + " bytes, but the provided value only has " + size
                                + " bytes"));
            }
        });

        trait.getMax().ifPresent(max -> {
            if (size > max) {
                events.add(shapeValue.constraintsEvent(
                        "Value provided for `" + shape.getId()
                                + "` must have no more than " + max + " bytes, but the provided value has " + size
                                + " bytes"));
            }
        });
    }

    private void validateStringShapeValue(List<ValidationEvent> events, Shape shape, LengthTrait trait, ShapeValue shapeValue) {
        StringNode node = shapeValue.toNode().expectStringNode();

        trait.getMin().ifPresent(min -> {
            if (node.getValue().length() < min) {
                events.add(shapeValue.constraintsEvent(
                        String.format(
                                "String value provided for `%s` must be >= %d characters, but the provided value is "
                                        + "only %d characters.",
                                shape.getId(),
                                min,
                                node.getValue().length())));
            }
        });

        trait.getMax().ifPresent(max -> {
            if (node.getValue().length() > max) {
                events.add(shapeValue.constraintsEvent(
                        String.format(
                                "String value provided for `%s` must be <= %d characters, but the provided value is "
                                        + "%d characters.",
                                shape.getId(),
                                max,
                                node.getValue().length())));
            }
        });
    }
}
