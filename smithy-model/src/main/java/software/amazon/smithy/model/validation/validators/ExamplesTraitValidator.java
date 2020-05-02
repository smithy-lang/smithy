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

package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
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
        for (Shape shape : model.getShapesWithTrait(ExamplesTrait.class)) {
            shape.asOperationShape().ifPresent(operation -> {
                events.addAll(validateExamples(model, operation, operation.expectTrait(ExamplesTrait.class)));
            });
        }

        return events;
    }

    private List<ValidationEvent> validateExamples(Model model, OperationShape shape, ExamplesTrait trait) {
        List<ValidationEvent> events = new ArrayList<>();
        List<ExamplesTrait.Example> examples = trait.getExamples();

        for (ExamplesTrait.Example example : examples) {
            if (shape.getInput().isPresent()) {
                model.getShape(shape.getInput().get()).ifPresent(input -> {
                    NodeValidationVisitor validator = createVisitor(
                            "input", example.getInput(), model, shape, example);
                    events.addAll(input.accept(validator));
                });
            } else if (!example.getInput().isEmpty()) {
                events.add(error(shape, trait, String.format("Input parameters provided for operation with no "
                                                             + "input structure members: `%s`", example.getTitle())));
            }
            if (shape.getOutput().isPresent()) {
                model.getShape(shape.getOutput().get()).ifPresent(output -> {
                    NodeValidationVisitor validator = createVisitor(
                            "output", example.getOutput(), model, shape, example);
                    events.addAll(output.accept(validator));
                });
            } else if (!example.getOutput().isEmpty()) {
                events.add(error(shape, trait, String.format(
                        "Output parameters provided for operation with no output structure members: `%s`",
                        example.getTitle())));
            }
        }

        return events;
    }

    private NodeValidationVisitor createVisitor(
            String name,
            ObjectNode value,
            Model model,
            Shape shape,
            ExamplesTrait.Example example
    ) {
        return NodeValidationVisitor.builder()
                .model(model)
                .eventShapeId(shape.getId())
                .value(value)
                .startingContext("Example " + name + " of `" + example.getTitle() + "`")
                .eventId(getName())
                .build();
    }
}
