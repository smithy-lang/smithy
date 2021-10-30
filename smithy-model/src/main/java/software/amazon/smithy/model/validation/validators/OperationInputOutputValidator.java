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
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.neighbor.RelationshipType;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.OutputTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * Detects when a structure marked with the input trait or output
 * trait is used in other contexts than input or output or reused
 * by multiple operations.
 *
 * <p>{@code OperationInputOutputName} is emitted as a WARNING when the
 * input or output shape name does not start with the name of the
 * operation that targets it (if any). This event ID is intentionally
 * different from {@code OperationInputOutput} to allow it to be suppressed
 * separately from the ERROR events emitted from {@code OperationInputOutput}.
 */
public final class OperationInputOutputValidator extends AbstractValidator {

    private static final String OPERATION_INPUT_OUTPUT_NAME = "OperationInputOutputName";

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        NeighborProvider reverseProvider = NeighborProviderIndex.of(model).getReverseProvider();
        validateInputOutput(model.getShapesWithTrait(InputTrait.class), reverseProvider, events, "input", "output");
        validateInputOutput(model.getShapesWithTrait(OutputTrait.class), reverseProvider, events, "output", "input");
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
        return error(operation, "Operation " + property + " cannot target structures marked with the @"
                                + invalid + " trait");
    }

    private ValidationEvent emitInvalidMemberRef(MemberShape member, String trait) {
        return error(member, "This member illegally references a structure marked with the @"
                             + trait + " trait: " + member.getTarget());
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

    private ValidationEvent emitMultipleUses(Shape shape, String descriptor, Set<ShapeId> operations) {
        return error(shape, "This shape is marked with the @" + descriptor + " trait but is used illegally by "
                            + "multiple operations: " + ValidationUtils.tickedList(operations));
    }
}
