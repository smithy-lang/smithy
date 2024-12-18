/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.diff.ChangedShape;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Emits a warning when an error is added to a service.
 */
public final class AddedServiceError extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return differences.changedShapes(ServiceShape.class)
                .flatMap(change -> createErrorViolations(change).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> createErrorViolations(ChangedShape<ServiceShape> change) {
        if (change.getOldShape().getErrors().equals(change.getNewShape().getErrors())) {
            return Collections.emptyList();
        }

        List<ValidationEvent> events = new ArrayList<>();
        for (ShapeId id : change.getNewShape().getErrors()) {
            if (!change.getOldShape().getErrors().contains(id)) {
                events.add(warning(change.getNewShape(),
                        String.format(
                                "The `%s` error was added to the `%s` service, making this error common "
                                        + "to all operations within the service. This is backward-compatible if the "
                                        + "error is only encountered as a result of a change in behavior of "
                                        + "the client (for example, the client sends a new "
                                        + "parameter to an operation).",
                                id,
                                change.getShapeId())));
            }
        }

        return events;
    }
}
