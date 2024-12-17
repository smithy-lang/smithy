/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Emits an ERROR when the input shape of an operation is changed to
 * another shape.
 */
public final class ChangedOperationInput extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return differences.changedShapes(OperationShape.class)
                .filter(change -> !change.getOldShape().getInputShape().equals(change.getNewShape().getInputShape()))
                .map(change -> error(change.getNewShape(),
                        String.format(
                                "Changed operation input of `%s` from `%s` to `%s`",
                                change.getShapeId(),
                                change.getOldShape().getInputShape(),
                                change.getNewShape().getInputShape())))
                .collect(Collectors.toList());
    }
}
