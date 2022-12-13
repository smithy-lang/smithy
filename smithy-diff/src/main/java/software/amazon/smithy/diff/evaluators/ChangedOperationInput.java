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
                .map(change -> error(change.getNewShape(), String.format(
                        "Changed operation input of `%s` from `%s` to `%s`",
                        change.getShapeId(),
                        change.getOldShape().getInputShape(),
                        change.getNewShape().getInputShape())))
                .collect(Collectors.toList());
    }
}
