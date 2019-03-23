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
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.SimpleShape;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Checks for changes in the shapes targeted by a member.
 *
 * <p>If the shape targeted by the member changes from a simple shape to
 * a simple shape of the same type with the same traits, then the emitted
 * event is a WARNING. All other changes are ERROR events.
 */
public class ChangedMemberTarget extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return differences.changedShapes(MemberShape.class)
                .filter(change -> !change.getOldShape().getTarget().equals(change.getNewShape().getTarget()))
                .map(change -> createChangeEvent(differences, change))
                .collect(Collectors.toList());
    }

    private ValidationEvent createChangeEvent(Differences differences, ChangedShape<MemberShape> change) {
        Shape oldTarget = getShapeTarget(differences.getOldModel(), change.getOldShape().getTarget());
        Shape newTarget = getShapeTarget(differences.getNewModel(), change.getNewShape().getTarget());
        Severity severity = areShapesCompatible(oldTarget, newTarget) ? Severity.WARNING : Severity.ERROR;

        return ValidationEvent.builder()
                .severity(severity)
                .eventId(getEventId())
                .shape(change.getNewShape())
                .message(createMessage(change, oldTarget, newTarget))
                .build();
    }

    private Shape getShapeTarget(Model model, ShapeId id) {
        return model.getShapeIndex().getShape(id).orElse(null);
    }

    private boolean areShapesCompatible(Shape oldShape, Shape newShape) {
        if (oldShape == null || newShape == null) {
            return false;
        }

        return oldShape.getType() == newShape.getType()
               && oldShape instanceof SimpleShape
               && newShape instanceof SimpleShape
               && oldShape.getAllTraits().equals(newShape.getAllTraits());
    }

    private String createMessage(ChangedShape<MemberShape> change, Shape oldTarget, Shape newTarget) {
        return String.format(
                "The shape targeted by the member `%s` changed from `%s`, a %s, to `%s`, a %s.",
                change.getShapeId(),
                change.getOldShape().getTarget(),
                oldTarget,
                change.getNewShape().getTarget(),
                newTarget);
    }
}
