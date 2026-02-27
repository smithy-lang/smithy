/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.internal.NodeHandler;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.ShapeExamplesTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;

/**
 * Emits a validation event if a shape examples conflict with their configured validation traits.
 */
public final class ShapeExamplesTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (Shape shape : model.getShapesWithTrait(ShapeExamplesTrait.class)) {
            validateTestValuesTrait(events, model, shape);
        }

        return events;
    }

    private void validateTestValuesTrait(List<ValidationEvent> events, Model model, Shape shape) {
        ShapeExamplesTrait trait = shape.expectTrait(ShapeExamplesTrait.class);

        List<Node> allowedValueNodes = trait.getAllowed().orElseGet(ListUtils::of);
        for (int index = 0; index < allowedValueNodes.size(); index += 1) {
            Node allowedValueNode = allowedValueNodes.get(index);

            NodeValidationVisitor visitor = NodeValidationVisitor.builder()
                    .model(model)
                    .eventId(getName() + ".allowed." + index)
                    .eventShapeId(shape.getId())
                    .value(allowedValueNode)
                    .startingContext(String.format("Allowed shape example `%s`",
                            NodeHandler.print(allowedValueNode)))
                    .addFeature(NodeValidationVisitor.Feature.REQUIRE_BASE_64_BLOB_VALUES)
                    .build();

            events.addAll(shape.accept(visitor));
        }

        List<Node> disallowedValueNodes = trait.getDisallowed().orElseGet(ListUtils::of);
        for (int index = 0; index < disallowedValueNodes.size(); index += 1) {
            Node disallowedValueNode = disallowedValueNodes.get(index);

            NodeValidationVisitor visitor = NodeValidationVisitor.builder()
                    .model(model)
                    .eventId(getName() + ".disallowed." + index)
                    .eventShapeId(shape.getId())
                    .value(disallowedValueNode)
                    .startingContext(String.format("Disallowed shape example `%s`",
                            NodeHandler.print(disallowedValueNode)))
                    .addFeature(NodeValidationVisitor.Feature.REQUIRE_BASE_64_BLOB_VALUES)
                    .build();

            List<ValidationEvent> validationEvents = shape.accept(visitor);
            List<ValidationEvent> nonErrorValidationEvents = validationEvents.stream()
                    .filter(validationEvent -> validationEvent.getSeverity() != Severity.ERROR)
                    .collect(Collectors.toList());

            events.addAll(nonErrorValidationEvents);

            if (validationEvents.size() == nonErrorValidationEvents.size()) {
                events.add(error(shape,
                        disallowedValueNode,
                        String.format("Disallowed shape example `%s` passed all validations when it shouldn't have",
                                NodeHandler.print(disallowedValueNode)),
                        "disallowed",
                        Integer.toString(index)));
            }
        }
    }
}
