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
import software.amazon.smithy.diff.ChangedShape;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.UniqueItemsTrait;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Creates an ERROR event when the type of a shape changes.
 */
public final class ChangedShapeType extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return differences.changedShapes()
                .filter(diff -> diff.getOldShape().getType() != diff.getNewShape().getType())
                .filter(diff -> !expectedSetToListChange(diff))
                .map(diff -> error(diff.getNewShape(), String.format(
                        "Shape `%s` type was changed from `%s` to `%s`.",
                        diff.getShapeId(), diff.getOldShape().getType(), diff.getNewShape().getType())))
                .collect(Collectors.toList());
    }

    private boolean expectedSetToListChange(ChangedShape<Shape> diff) {
        ShapeType oldType = diff.getOldShape().getType();
        ShapeType newType = diff.getNewShape().getType();

        // Smithy diff doesn't raise an issue if a set is changed to a list and the list
        // has the uniqueItems trait. Set is deprecated and this is a recommended change.
        if (oldType == ShapeType.SET && newType == ShapeType.LIST) {
            return diff.getNewShape().hasTrait(UniqueItemsTrait.class);
        }

        return false;
    }
}
