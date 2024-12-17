/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.OutputTrait;
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
 *     <li>Emits an {@code OperationInputOutputMisuse} ERROR when a structure
 *     marked with the {@code input} trait or {@code output} trait is used in
 *     other contexts than input or output, or reused by multiple operations.</li>
 *     <li>Emits an {@code OperationInputOutputName} WARNING when the input or
 *     output shape name does not start with the name of the operation that
 *     targets it (if any).</li>
 *     <li>Emits an {@code OperationNameAmbiguity} WARNING when a shape has a
 *     name that starts with the name of an operation and the name ends with
 *     Input, Output, Request, or Response but is not used as the input or
 *     output of an operation.</li>
 * </ul>
 */
public final class OperationValidator extends AbstractValidator {

    private static final String OPERATION_INPUT_OUTPUT_MISUSE = "OperationInputOutputMisuse";
    private static final String OPERATION_INPUT_OUTPUT_NAME = "OperationInputOutputName";
    private static final String OPERATION_NAME_AMBIGUITY = "OperationNameAmbiguity";
    private static final List<String> INPUT_SUFFIXES = ListUtils.of("Input", "Request");
    private static final List<String> OUTPUT_SUFFIXES = ListUtils.of("Output", "Response");

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        NeighborProvider reverseProvider = NeighborProviderIndex.of(model).getReverseProvider();
        validateInputOutput(model.getShapesWithTrait(InputTrait.class), reverseProvider, events, "input", "output");
        validateInputOutput(model.getShapesWithTrait(OutputTrait.class), reverseProvider, events, "output", "input");

        for (OperationShape operation : model.getOperationShapes()) {
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
                    events.add(emitInvalidOperationBinding(rel.getShape(), shape, relName, descriptor));
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

    private ValidationEvent emitInvalidOperationBinding(
            Shape operation,
            Shape target,
            String property,
            String invalid
    ) {
        return ValidationEvent.builder()
                .id(OPERATION_INPUT_OUTPUT_MISUSE)
                .severity(Severity.ERROR)
                .shape(operation)
                .message(String.format(
                        "Operation `%s` cannot target structures marked with the `@%s` trait: `%s`",
                        property,
                        invalid,
                        target.getId()))
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
                .id(OPERATION_INPUT_OUTPUT_NAME + "." + property)
                .message(String.format(
                        "The %s of this operation should target a shape that starts with the operation's name, '%s', "
                                + "but the targeted shape is `%s`",
                        property,
                        operation.getId().getName(),
                        target))
                .build();
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
                                + "types.",
                        descriptor,
                        operation.getId(),
                        ioShape))
                .build();
    }
}
