/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.PrivateTrait;
import software.amazon.smithy.model.traits.synthetic.SyntheticShapeTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Creates an ERROR event when a non-private non-scalar shape is removed.
 * Creates a WARNING event when a non-private scalar shape is removed.
 * Creates a NOTE event when all references to a removed shape were replaced
 * by inline collection syntax and no non-trivial traits were lost.
 */
public final class RemovedShape extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return differences.removedShapes()
                .filter(shape -> !shape.hasTrait(PrivateTrait.ID))
                // Synthetic shapes are assembler-generated implementation details, not API surface.
                .filter(shape -> !shape.hasTrait(SyntheticShapeTrait.ID))
                .filter(shape -> !isMemberOfRemovedShape(shape, differences))
                .map(shape -> createEvent(shape, differences))
                .collect(Collectors.toList());
    }

    private ValidationEvent createEvent(Shape shape, Differences differences) {
        if (isInconsequentialType(shape)) {
            return ValidationEvent.builder()
                    .severity(Severity.WARNING)
                    .message(String.format("Removed %s `%s`", shape.getType(), shape.getId()))
                    .shapeId(shape.getId())
                    .id(getEventId() + ".ScalarShape")
                    .sourceLocation(shape.getSourceLocation())
                    .build();
        }

        if (isReplacedByInlineCollection(shape, differences)) {
            if (hasNonSyntheticTraits(shape)) {
                return error(shape,
                        String.format(
                                "Removed %s `%s` which had traits that cannot be expressed using "
                                        + "inline collection syntax. The following traits were lost: %s",
                                shape.getType(),
                                shape.getId(),
                                getNonSyntheticTraitIds(shape)));
            }
            return note(shape,
                    String.format(
                            "Removed %s `%s`. All previous references now use inline collection syntax. "
                                    + "See the ChangedMemberTarget evaluator for any effective differences.",
                            shape.getType(),
                            shape.getId()));
        }

        return error(shape, String.format("Removed %s `%s`", shape.getType(), shape.getId()));
    }

    private boolean isReplacedByInlineCollection(Shape shape, Differences differences) {
        ShapeType type = shape.getType();
        if (type != ShapeType.LIST && type != ShapeType.MAP) {
            return false;
        }

        // Find all members in the old model that targeted this shape.
        ShapeId removedId = shape.getId();
        List<MemberShape> oldReferences = differences.getOldModel()
                .getMemberShapes()
                .stream()
                .filter(m -> m.getTarget().equals(removedId))
                .collect(Collectors.toList());

        if (oldReferences.isEmpty()) {
            return false;
        }

        // Check that every previous reference now targets a synthetic shape.
        for (MemberShape oldMember : oldReferences) {
            MemberShape newMember = differences.getNewModel()
                    .getShape(oldMember.getId())
                    .flatMap(Shape::asMemberShape)
                    .orElse(null);
            if (newMember == null) {
                continue; // Member was also removed, that's fine.
            }
            Shape newTarget = differences.getNewModel()
                    .getShape(newMember.getTarget())
                    .orElse(null);
            if (newTarget == null || !newTarget.hasTrait(SyntheticShapeTrait.ID)) {
                return false;
            }
        }

        return true;
    }

    private boolean hasNonSyntheticTraits(Shape shape) {
        return shape.getAllTraits()
                .values()
                .stream()
                .anyMatch(t -> !t.isSynthetic());
    }

    private String getNonSyntheticTraitIds(Shape shape) {
        return shape.getAllTraits()
                .entrySet()
                .stream()
                .filter(e -> !e.getValue().isSynthetic())
                .map(Map.Entry::getKey)
                .map(ShapeId::toString)
                .collect(Collectors.joining(", "));
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
