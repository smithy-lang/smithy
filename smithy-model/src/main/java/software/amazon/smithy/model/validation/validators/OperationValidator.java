/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.knowledge.OperationIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.OutputTrait;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.ListUtils;

/**
 * Validates operation inputs, outputs, and the use of {@code input}
 * and {@code output} traits.
 *
 * <ul>
 *     <li>Emits an {@code OperationMissingInputTrait} WARNING when the input
 *     of an operation is not marked with the {@code input} trait.</li>
 *     <li>Emits an {@code OperationMissingOutputTrait} WARNING when the
 *     output of an operation is not marked with the {@code output} trait.</li>
 *     <li>Emits an {@code OperationInputOutputMisuse} ERROR when a structure
 *     marked with the {@code input} trait or {@code output} trait is used in
 *     other contexts than input or output, or reused by multiple operations.</li>
 *     <li>Emits an {@code OperationInputOutputName} WARNING when the input or
 *     output shape name does not start with the name of the operation that
 *     targets it (if any).</li>
 *     <li>Emits an {@code OperationNameAmbiguity} WARNING when a shape has
 *     name that starts with the name of an operation and the name ends with
 *     Input, Output, Request, or Response but is not used as the input or
 *     output of an operation.</li>
 * </ul>
 */
public final class OperationValidator extends AbstractValidator {

    private static final String OPERATION_INPUT_OUTPUT_MISUSE = "OperationInputOutputMisuse";
    private static final String OPERATION_INPUT_OUTPUT_NAME = "OperationInputOutputName";
    private static final String OPERATION_MISSING_INPUT_TRAIT = "OperationMissingInputTrait";
    private static final String OPERATION_MISSING_OUTPUT_TRAIT = "OperationMissingOutputTrait";
    private static final String OPERATION_NAME_AMBIGUITY = "OperationNameAmbiguity";
    private static final List<String> INPUT_SUFFIXES = ListUtils.of("Input", "Request");
    private static final List<String> OUTPUT_SUFFIXES = ListUtils.of("Output", "Response");

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        NeighborProvider reverseProvider = NeighborProviderIndex.of(model).getReverseProvider();
        validateInputOutput(model.getShapesWithTrait(InputTrait.class), reverseProvider, events, "input", "output");
        validateInputOutput(model.getShapesWithTrait(OutputTrait.class), reverseProvider, events, "output", "input");

        OperationIndex index = OperationIndex.of(model);
        for (OperationShape operation : model.getOperationShapes()) {
            StructureShape input = index.expectInputShape(operation);
            StructureShape output = index.expectOutputShape(operation);
            validateInputOutputSet(operation, input, output, events);
            validateOperationNameAmbiguity(model, operation, events);
        }

        return events;
    }

    private void validateInputOutput(
            Set<Shape> shapes,
            NeighborProvider reverseProvider,
            List<ValidationEvent> events,
            String descriptor,
            String invalid
    ) {
        for (Shape shape : shapes) {
            Set<ShapeId> operations = new HashSet<>();
            for (Relationship rel : reverseProvider.getNeighbors(shape)) {
                String relName = rel.getSelectorLabel().orElse("");
                if (relName.equals(descriptor)) {
                    // Ensure there's one input/output target.
                    operations.add(rel.getShape().getId());
                    // Make sure the operation name is part of the target shape ID.
                    if (!rel.getNeighborShapeId().getName().startsWith(rel.getShape().getId().getName())) {
                        events.add(emitBadInputOutputName(rel.getShape(), descriptor, rel.getNeighborShapeId()));
                    }
                } else if (relName.equals(invalid)) {
                    // Input shouldn't reference output, and vice versa.
                    events.add(emitInvalidOperationBinding(rel.getShape(), descriptor, invalid));
                } else if (rel.getRelationshipType() == RelationshipType.MEMBER_TARGET) {
                    // Members can't target shapes marked with @input or @output.
                    events.add(emitInvalidMemberRef(rel.getShape().asMemberShape().get(), descriptor));
                }
            }

            // Only a single shape can target an @input|@output shape.
            if (operations.size() > 1) {
                events.add(emitMultipleUses(shape, descriptor, operations));
            }
        }
    }

    private ValidationEvent emitInvalidOperationBinding(Shape operation, String property, String invalid) {
        return ValidationEvent.builder()
                .id(OPERATION_INPUT_OUTPUT_MISUSE)
                .severity(Severity.ERROR)
                .shape(operation)
                .message("Operation " + property + " cannot target structures marked with the @" + invalid + " trait")
                .build();
    }

    private ValidationEvent emitInvalidMemberRef(MemberShape member, String trait) {
        return ValidationEvent.builder()
                .id(OPERATION_INPUT_OUTPUT_MISUSE)
                .severity(Severity.ERROR)
                .shape(member)
                .message("Members cannot target structures marked with the @" + trait + " trait: " + member.getTarget())
                .build();
    }

    private ValidationEvent emitMultipleUses(Shape shape, String descriptor, Set<ShapeId> operations) {
        return ValidationEvent.builder()
                .id(OPERATION_INPUT_OUTPUT_MISUSE)
                .severity(Severity.ERROR)
                .shape(shape)
                .message("Shapes marked with the @" + descriptor + " trait cannot be used as " + descriptor + " by "
                         + "multiple operations: " + ValidationUtils.tickedList(operations))
                .build();
    }

    private ValidationEvent emitBadInputOutputName(Shape operation, String property, ShapeId target) {
        return ValidationEvent.builder()
                .severity(Severity.WARNING)
                .shape(operation)
                .id(OPERATION_INPUT_OUTPUT_NAME)
                .message(String.format(
                        "The %s of this operation should target a shape that starts with the operation's name, '%s', "
                        + "but the targeted shape is `%s`", property, operation.getId().getName(), target))
                .build();
    }

    private void validateInputOutputSet(
            OperationShape operation,
            StructureShape input,
            StructureShape output,
            List<ValidationEvent> events
    ) {
        if (!input.getId().equals(UnitTypeTrait.UNIT) && !input.hasTrait(InputTrait.class)) {
            events.add(ValidationEvent.builder()
                               .id(OPERATION_MISSING_INPUT_TRAIT)
                               .severity(Severity.WARNING)
                               .shape(input)
                               .message(String.format(
                                       "This structure is the input of `%s`, but it is not marked with the "
                                       + "@input trait. The @input trait gives operations more flexibility to "
                                       + "evolve their top-level input members in ways that would otherwise "
                                       + "be backward incompatible.", operation.getId()))
                               .build());
        }

        if (!output.getId().equals(UnitTypeTrait.UNIT) && !output.hasTrait(OutputTrait.class)) {
            events.add(ValidationEvent.builder()
                               .id(OPERATION_MISSING_OUTPUT_TRAIT)
                               .severity(Severity.WARNING)
                               .shape(output)
                               .message(String.format(
                                       "This structure is the output of `%s`, but it is not marked with "
                                       + "the @output trait.", operation.getId()))
                               .build());
        }
    }

    private void validateOperationNameAmbiguity(Model model, OperationShape operation, List<ValidationEvent> events) {
        ShapeId input = operation.getInputShape();
        for (String suffix : INPUT_SUFFIXES) {
            ShapeId test = ShapeId.from(operation.getId().toShapeId() + suffix);
            if (!test.equals(input)) {
                model.getShape(test).ifPresent(ambiguousShape -> {
                    events.add(createAmbiguousEvent(ambiguousShape, operation, input, "input"));
                });
            }
        }

        ShapeId output = operation.getOutputShape();
        for (String suffix : OUTPUT_SUFFIXES) {
            ShapeId test = ShapeId.from(operation.getId().toShapeId() + suffix);
            if (!test.equals(output)) {
                model.getShape(test).ifPresent(ambiguousShape -> {
                    events.add(createAmbiguousEvent(ambiguousShape, operation, output, "output"));
                });
            }
        }
    }

    private ValidationEvent createAmbiguousEvent(
            Shape ambiguousShape,
            OperationShape operation,
            ShapeId ioShape,
            String descriptor
    ) {
        return ValidationEvent.builder()
                .id(OPERATION_NAME_AMBIGUITY)
                .shape(ambiguousShape)
                .severity(Severity.WARNING)
                .message(String.format(
                        "The name of this shape implies that it is the %1$s of %2$s, but that operation uses %3$s "
                        + "for %1$s. This kind of ambiguity can confuse developers calling this operation and can "
                        + "cause issues in code generators that use similar naming conventions to generate %1$s "
                        + "types.", descriptor, operation.getId(), ioShape))
                .build();
    }
}
