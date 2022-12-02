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
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Creates a NOTE event when a shape is added.
 */
public final class AddedShape extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return differences.addedShapes()
                .filter(shape -> !isMemberOfAddedShape(shape, differences))
                .filter(shape -> !isMemberOfConvertedEnumShape(shape, differences))
                .map(shape -> note(shape, String.format("Added %s `%s`", shape.getType(), shape.getId())))
                .collect(Collectors.toList());
    }

    private boolean isMemberOfAddedShape(Shape shape, Differences differences) {
        return shape.asMemberShape()
                .filter(member -> !differences.getOldModel().getShapeIds().contains(member.getContainer()))
                .isPresent();
    }

    private boolean isMemberOfConvertedEnumShape(Shape shape, Differences differences) {
        if (!shape.asMemberShape().isPresent()) {
            return false;
        }

        ShapeId conversionShapeId = shape.asMemberShape().get().getContainer();

        Optional<StringShape> oldStringShapeWithEnumTrait = differences.getOldModel()
                .getShape(conversionShapeId)
                .flatMap(Shape::asStringShape)
                .filter(s -> s.getType() == ShapeType.STRING)
                .filter(s -> s.hasTrait(EnumTrait.ID));

        Optional<EnumShape> newEnumShape = differences.getNewModel()
                .getShape(conversionShapeId)
                .flatMap(Shape::asEnumShape);

        // Changes in enum values are handled in the ChangedEnumTrait evaluator
        return newEnumShape.isPresent() && oldStringShapeWithEnumTrait.isPresent();
    }
}
