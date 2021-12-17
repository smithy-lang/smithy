/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Emits a warning when a service is unbound from a service.
 */
public final class RemovedServiceBoundShape extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return differences.changedShapes(ServiceShape.class)
                .flatMap(change -> createShapeBindingEvents(change).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> createShapeBindingEvents(ChangedShape<ServiceShape> change) {
        if (change.getOldShape().getShapes().equals(change.getNewShape().getShapes())) {
            return Collections.emptyList();
        }

        List<ValidationEvent> events = new ArrayList<>();
        for (ShapeId id : change.getOldShape().getShapes()) {
            if (!change.getNewShape().getShapes().contains(id)) {
                events.add(warning(change.getNewShape(), String.format(
                        "The `%s` shape was unbound from the `%s` service.",
                        id, change.getShapeId())));
            }
        }

        return events;
    }
}
