/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.TopDownIndex;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.ExamplesTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.NodeValidationVisitor;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that examples traits are valid for their operations.
 */
public final class ExamplesTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (OperationShape operation : model.getOperationShapesWithTrait(ExamplesTrait.class)) {
            events.addAll(validateExamples(model, operation, operation.expectTrait(ExamplesTrait.class)));
        }

        return events;
    }

    private List<ValidationEvent> validateExamples(Model model, OperationShape shape, ExamplesTrait trait) {
        List<ValidationEvent> events = new ArrayList<>();
        List<ExamplesTrait.Example> examples = trait.getExamples();

        for (ExamplesTrait.Example example : examples) {
            boolean isOutputDefined = example.getOutput().isPresent();
            boolean isErrorDefined = example.getError().isPresent();

            model.getShape(shape.getInputShape()).ifPresent(input -> {
                NodeValidationVisitor validator;
                if (example.getAllowConstraintErrors() && !isErrorDefined) {
                    events.add(error(shape,
                            trait,
                            String.format(
                                    "Example: `%s` has allowConstraintErrors enabled, so error must be defined.",
                                    example.getTitle())));
                }
                validator = createVisitor("input", example.getInput(), model, shape, example);
                List<ValidationEvent> inputValidationEvents = input.accept(validator);
                events.addAll(inputValidationEvents);
            });

            if (isOutputDefined && isErrorDefined) {
                events.add(error(shape,
                        trait,
                        String.format(
                                "Example: `%s` has both output and error defined, only one should be present.",
                                example.getTitle())));
            } else if (isOutputDefined) {
                model.getShape(shape.getOutputShape()).ifPresent(output -> {
                    NodeValidationVisitor validator = createVisitor(
                            "output",
                            example.getOutput().get(),
                            model,
                            shape,
                            example);
                    events.addAll(output.accept(validator));
                });
            } else if (isErrorDefined) {
                ExamplesTrait.ErrorExample errorExample = example.getError().get();
                Optional<Shape> errorShape = model.getShape(errorExample.getShapeId());
                if (errorShape.isPresent() && (
                // The error is directly bound to the operation.
                shape.getErrors().contains(errorExample.getShapeId())
                        // The error is bound to all services that contain the operation.
                        || servicesContainError(model, shape, errorExample.getShapeId()))) {
                    NodeValidationVisitor validator = createVisitor(
                            "error",
                            errorExample.getContent(),
                            model,
                            shape,
                            example);
                    events.addAll(errorShape.get().accept(validator));
                } else {
                    events.add(error(shape,
                            trait,
                            String.format(
                                    "Error parameters provided for operation without the `%s` error: `%s`",
                                    errorExample.getShapeId(),
                                    example.getTitle())));
                }
            }
        }

        return events;
    }

    private boolean servicesContainError(Model model, OperationShape shape, ShapeId errorId) {
        TopDownIndex topDownIndex = TopDownIndex.of(model);

        Set<ServiceShape> services = model.getServiceShapes();
        if (services.isEmpty()) {
            return false;
        }

        for (ServiceShape service : services) {
            // Skip if the service doesn't have the operation.
            if (!topDownIndex.getContainedOperations(service).contains(shape)) {
                continue;
            }

            // We've already checked if the operation contains the error,
            // so a service having no errors means we've failed.
            if (service.getErrors().isEmpty() || !service.getErrors().contains(errorId)) {
                return false;
            }
        }

        return true;
    }

    private NodeValidationVisitor createVisitor(
            String name,
            ObjectNode value,
            Model model,
            Shape shape,
            ExamplesTrait.Example example
    ) {
        NodeValidationVisitor.Builder builder = NodeValidationVisitor.builder()
                .model(model)
                .eventShapeId(shape.getId())
                .value(value)
                .startingContext("Example " + name + " of `" + example.getTitle() + "`")
                .eventId(getName());
        if (example.getAllowConstraintErrors()) {
            builder.addFeature(NodeValidationVisitor.Feature.ALLOW_CONSTRAINT_ERRORS);
        }
        return builder.build();
    }
}
