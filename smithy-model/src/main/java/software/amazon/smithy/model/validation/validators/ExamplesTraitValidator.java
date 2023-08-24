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

import static software.amazon.smithy.model.validation.NodeValidationVisitor.Feature.BLOB_LENGTH_WARNING;
import static software.amazon.smithy.model.validation.NodeValidationVisitor.Feature.MAP_LENGTH_WARNING;
import static software.amazon.smithy.model.validation.NodeValidationVisitor.Feature.PATTERN_TRAIT_WARNING;
import static software.amazon.smithy.model.validation.NodeValidationVisitor.Feature.RANGE_TRAIT_WARNING;
import static software.amazon.smithy.model.validation.NodeValidationVisitor.Feature.REQUIRED_TRAIT_WARNING;
import static software.amazon.smithy.model.validation.NodeValidationVisitor.Feature.STRING_LENGTH_WARNING;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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

    private static final Set<NodeValidationVisitor.Feature> ALLOWED_FEATURES = EnumSet.of(
            BLOB_LENGTH_WARNING,
            MAP_LENGTH_WARNING,
            PATTERN_TRAIT_WARNING,
            RANGE_TRAIT_WARNING,
            REQUIRED_TRAIT_WARNING,
            STRING_LENGTH_WARNING);

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
                if (example.getLowerInputValidationSeverity().isPresent()
                        && !example.getLowerInputValidationSeverity().get().isEmpty()) {
                    if (!isErrorDefined) {
                        events.add(error(shape, trait, String.format(
                            "Example: `%s` has lowerInputValidationSeverity defined, so error must also be defined.",
                            example.getTitle())));
                    }
                    validator = createVisitor("input", example.getInput(), model, shape, example, true);
                } else {
                    validator = createVisitor("input", example.getInput(), model, shape, example, false);
                }
                List<ValidationEvent> inputValidationEvents = input.accept(validator);
                events.addAll(inputValidationEvents);
            });

            if (isOutputDefined && isErrorDefined) {
                events.add(error(shape, trait, String.format(
                        "Example: `%s` has both output and error defined, only one should be present.",
                        example.getTitle())));
            } else if (isOutputDefined) {
                model.getShape(shape.getOutputShape()).ifPresent(output -> {
                    NodeValidationVisitor validator = createVisitor(
                            "output", example.getOutput().get(), model, shape, example, false);
                    events.addAll(output.accept(validator));
                });
            } else if (isErrorDefined) {
                ExamplesTrait.ErrorExample errorExample = example.getError().get();
                Optional<Shape> errorShape = model.getShape(errorExample.getShapeId());
                if (errorShape.isPresent() && shape.getErrors().contains(errorExample.getShapeId())) {
                    NodeValidationVisitor validator = createVisitor(
                            "error", errorExample.getContent(), model, shape, example, false);
                    events.addAll(errorShape.get().accept(validator));
                } else {
                    events.add(error(shape, trait, String.format(
                            "Error parameters provided for operation without the `%s` error: `%s`",
                            errorExample.getShapeId(), example.getTitle())));
                }
            }
        }

        return events;
    }

    private NodeValidationVisitor createVisitor(
            String name,
            ObjectNode value,
            Model model,
            Shape shape,
            ExamplesTrait.Example example,
            boolean enableFeatures
    ) {
        NodeValidationVisitor.Builder builder = NodeValidationVisitor.builder()
                .model(model)
                .eventShapeId(shape.getId())
                .value(value)
                .startingContext("Example " + name + " of `" + example.getTitle() + "`")
                .eventId(getName());
        if (enableFeatures) {
            example.getLowerInputValidationSeverity().ifPresent(features -> features.stream()
                    .filter(ALLOWED_FEATURES::contains)
                    .forEach(builder::addFeature));
        }
        return builder.build();
    }
}
