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
 * Emits a warning when an error is removed from a service.
 */
public final class RemovedServiceError extends AbstractDiffEvaluator {
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
        for (ShapeId id : change.getOldShape().getErrors()) {
            if (!change.getNewShape().getErrors().contains(id)) {
                events.add(warning(change.getNewShape(),
                        String.format(
                                "The `%s` error was removed from the `%s` service. This means that it "
                                        + "is no longer considered an error common to all operations within the "
                                        + "service.",
                                change.getShapeId(),
                                id)));
            }
        }

        return events;
    }
}
