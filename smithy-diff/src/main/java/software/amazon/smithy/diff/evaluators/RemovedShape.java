/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.PrivateTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Creates an ERROR event when a non-private non-scalar shape is removed.
 * Creates a WARNING event when a non-private scalar shape is removed.
 */
public final class RemovedShape extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return differences.removedShapes()
                .filter(shape -> !shape.hasTrait(PrivateTrait.class))
                .filter(shape -> !isMemberOfRemovedShape(shape, differences))
                .map(shape -> isInconsequentialType(shape)
                        ? ValidationEvent.builder()
                                .severity(Severity.WARNING)
                                .message(String.format("Removed %s `%s`", shape.getType(), shape.getId()))
                                .shapeId(shape.getId())
                                .id(getEventId() + ".ScalarShape")
                                .sourceLocation(shape.getSourceLocation())
                                .build()
                        : error(shape, String.format("Removed %s `%s`", shape.getType(), shape.getId())))
                .collect(Collectors.toList());
    }

    private boolean isMemberOfRemovedShape(Shape shape, Differences differences) {
        return shape.asMemberShape()
                .filter(member -> !differences.getNewModel().getShapeIds().contains(member.getContainer()))
                .isPresent();
    }

    private boolean isInconsequentialType(Shape shape) {
        ShapeType shapeType = shape.getType();
        return shapeType == ShapeType.BIG_DECIMAL
                || shapeType == ShapeType.BIG_INTEGER
                || shapeType == ShapeType.BLOB
                || shapeType == ShapeType.BOOLEAN
                || shapeType == ShapeType.BYTE
                || shapeType == ShapeType.DOUBLE
                || shapeType == ShapeType.FLOAT
                || shapeType == ShapeType.SHORT
                || shapeType == ShapeType.TIMESTAMP
                || shapeType == ShapeType.LONG
                || ((shapeType == ShapeType.STRING) && !shape.hasTrait(EnumTrait.class))
                || (shapeType == ShapeType.INTEGER);
    }
}
