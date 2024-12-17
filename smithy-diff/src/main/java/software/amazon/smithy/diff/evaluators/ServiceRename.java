/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.neighbor.Walker;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Creates an ERROR event when a shape is renamed that was already
 * part of the service, when a rename changes for a shape, or when
 * a rename is removed for a shape.
 */
public final class ServiceRename extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        Walker oldWalker = new Walker(differences.getOldModel());

        return differences.changedShapes(ServiceShape.class)
                .flatMap(diff -> {
                    ServiceShape oldShape = diff.getOldShape();
                    ServiceShape newShape = diff.getNewShape();
                    if (oldShape.getRename().equals(newShape.getRename())) {
                        return Stream.empty();
                    }

                    // Look for the removal or changing of old renames.
                    List<ValidationEvent> events = new ArrayList<>();
                    for (Map.Entry<ShapeId, String> old : oldShape.getRename().entrySet()) {
                        String newValue = newShape.getRename().get(old.getKey());
                        if (newValue == null) {
                            events.add(error(newShape,
                                    String.format(
                                            "Service rename of `%s` to `%s` was removed",
                                            old.getKey(),
                                            old.getValue())));
                        } else if (!old.getValue().equals(newValue)) {
                            events.add(error(newShape,
                                    String.format(
                                            "Service rename of `%s` was changed from `%s` to `%s`",
                                            old.getKey(),
                                            old.getValue(),
                                            newValue)));
                        }
                    }

                    // Look for the addition of new renames to shapes already in the closure.
                    Set<ShapeId> oldClosure = oldWalker.walkShapeIds(oldShape);
                    for (Map.Entry<ShapeId, String> newEntry : newShape.getRename().entrySet()) {
                        if (!oldShape.getRename().containsKey(newEntry.getKey())) {
                            if (oldClosure.contains(newEntry.getKey())) {
                                events.add(error(newShape,
                                        String.format(
                                                "Service rename of `%s` to `%s` was added to an old shape",
                                                newEntry.getKey(),
                                                newEntry.getValue())));
                            }
                        }
                    }

                    return events.stream();
                })
                .collect(Collectors.toList());
    }
}
