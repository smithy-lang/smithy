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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.diff.ChangedShape;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Emits a warning when an error is removed from an operation.
 */
public final class RemovedOperationError extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return differences.changedShapes(OperationShape.class)
                .flatMap(change -> createErrorViolations(change).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> createErrorViolations(ChangedShape<OperationShape> change) {
        if (change.getOldShape().getErrors().equals(change.getNewShape().getErrors())) {
            return Collections.emptyList();
        }

        List<ValidationEvent> events = new ArrayList<>();
        for (ShapeId error : change.getOldShape().getErrors()) {
            if (!change.getNewShape().getErrors().contains(error)) {
                events.add(
                        ValidationEvent.builder()
                                .id(getEventId() + "." + error.getName())
                                .severity(Severity.WARNING)
                                .message(String.format(
                                        "The `%s` error was removed from the `%s` operation.",
                                        error, change.getShapeId()))
                                .shape(change.getNewShape())
                                .build()
                );
            }
        }

        return events;
    }
}
