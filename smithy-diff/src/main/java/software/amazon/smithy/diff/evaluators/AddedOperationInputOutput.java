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

package software.amazon.smithy.diff.evaluators;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.diff.ChangedShape;
import software.amazon.smithy.diff.DiffEvaluator;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Meta-validator that emits a NOTE when a backward compatible operation
 * input or output is added (AddedOperationInput and AddedOperationOutput),
 * and an ERROR when a breaking operation input is added using the same
 * event IDs.
 *
 * <p>TODO: Also check for the addition of streaming and event streams.
 */
public final class AddedOperationInputOutput implements DiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return differences.changedShapes(OperationShape.class)
                .flatMap(change -> checkOperation(differences.getNewModel(), change).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> checkOperation(Model newModel, ChangedShape<OperationShape> change) {
        List<ValidationEvent> events = new ArrayList<>(2);
        if (!change.getOldShape().getInput().isPresent() && change.getNewShape().getInput().isPresent()) {
            validateChange("Input", newModel, change.getNewShape(), change.getNewShape().getInput().get())
                    .ifPresent(events::add);
        }

        if (!change.getOldShape().getOutput().isPresent() && change.getNewShape().getOutput().isPresent()) {
            validateChange("Output", newModel, change.getNewShape(), change.getNewShape().getOutput().get())
                    .ifPresent(events::add);
        }

        return events;
    }

    private Optional<ValidationEvent> validateChange(String rel, Model model, Shape operation, ShapeId target) {
        String eventId = "AddedOperation" + rel;

        return model.getShape(target).flatMap(Shape::asStructureShape).map(struct -> {
            if (struct.getAllMembers().values().stream().noneMatch(MemberShape::isRequired)) {
                // This is a backward compatible change.
                return ValidationEvent.builder()
                        .id(eventId)
                        .severity(Severity.NOTE)
                        .shape(operation)
                        .message(String.format(
                                "%s shape `%s` was added to the `%s` operation",
                                rel, target, operation.getId()))
                        .build();
            }

            // This is a breaking change!
            return ValidationEvent.builder()
                    .id(eventId)
                    .severity(Severity.ERROR)
                    .shape(operation)
                    .message(String.format(
                            "%s shape `%s` was added to the `%s` operation, and this structure contains "
                            + "one or more required members. %s can only be added to an operation if the targeted "
                            + "structure contains no required members.",
                            rel, target, operation.getId(), rel))
                    .build();
        });
    }
}
