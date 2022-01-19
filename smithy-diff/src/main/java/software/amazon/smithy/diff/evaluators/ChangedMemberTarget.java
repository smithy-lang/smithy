/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.stream.Collectors;
import software.amazon.smithy.diff.ChangedShape;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.SimpleShape;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SetUtils;

/**
 * Checks for changes in the shapes targeted by a member.
 *
 * <p>If the shape targeted by the member changes from a simple shape to
 * a simple shape of the same type with the same traits, or a list or set
 * that has a member that targets the shame exact shape and has the same
 * traits, then the emitted event is a WARNING. If an enum trait is
 * found on the old or newly targeted shape, then the event is an ERROR,
 * because enum traits typically materialize as named types in codegen.
 * All other changes are ERROR events.
 */
public final class ChangedMemberTarget extends AbstractDiffEvaluator {

    /**
     * These traits are traits that are known to significantly influence
     * code generation. Right now it just contains the enum trait, but
     * other traits could be added in the future as needed.
     */
    private static final Set<ShapeId> SIGNIFICANT_CODEGEN_TRAITS = SetUtils.of(EnumTrait.ID);

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
        List<String> issues = areShapesCompatible(oldTarget, newTarget);
        Severity severity = issues.isEmpty() ? Severity.WARNING : Severity.ERROR;

        String message = createSimpleMessage(change, oldTarget, newTarget);
        if (severity == Severity.WARNING) {
            message += "This was determined backward compatible.";
        } else {
            message += String.join(". ", issues) + ".";
        }

        return ValidationEvent.builder()
                .severity(severity)
                .id(getEventId())
                .shape(change.getNewShape())
                .message(message)
                .build();
    }

    private Shape getShapeTarget(Model model, ShapeId id) {
        return model.getShape(id).orElse(null);
    }

    private static List<String> areShapesCompatible(Shape oldShape, Shape newShape) {
        if (oldShape == null || newShape == null) {
            return ListUtils.of();
        }

        if (oldShape.getType() != newShape.getType()) {
            return ListUtils.of(String.format("The type of the targeted shape changed from %s to %s",
                                              oldShape.getType(), newShape.getType()));
        }

        if (!(oldShape instanceof SimpleShape || oldShape instanceof CollectionShape)) {
            return ListUtils.of(String.format("The name of a %s is significant", oldShape.getType()));
        }

        List<String> results = new ArrayList<>();
        for (ShapeId significantCodegenTrait : SIGNIFICANT_CODEGEN_TRAITS) {
            if (oldShape.hasTrait(significantCodegenTrait)) {
                results.add(String.format("The `%s` trait was found on the target, so the name of the targeted "
                                          + "shape matters for codegen",
                                          significantCodegenTrait));
            }
        }

        if (!oldShape.getAllTraits().equals(newShape.getAllTraits())) {
            results.add(createTraitDiffMessage(oldShape, newShape));
        }

        if (oldShape instanceof CollectionShape) {
            MemberShape oldMember = ((CollectionShape) oldShape).getMember();
            MemberShape newMember = ((CollectionShape) newShape).getMember();
            if (!oldMember.getTarget().equals(newMember.getTarget())) {
                results.add(String.format("Both the old and new shapes are a %s, but the old shape targeted "
                                          + "`%s` while the new shape targets `%s`",
                                          oldShape.getType(),
                                          oldMember.getTarget(),
                                          newMember.getTarget()));
            } else if (!oldMember.getAllTraits().equals(newMember.getAllTraits())) {
                results.add(String.format("Both the old and new shapes are a %s, but their members have "
                                          + "differing traits. %s",
                                          oldShape.getType(),
                                          createTraitDiffMessage(oldMember, newMember)));
            }
        }

        return results;
    }

    private static String createSimpleMessage(ChangedShape<MemberShape> change, Shape oldTarget, Shape newTarget) {
        return String.format(
                "The shape targeted by the member `%s` changed from `%s` (%s) to `%s` (%s). ",
                change.getShapeId(),
                change.getOldShape().getTarget(),
                oldTarget.getType(),
                change.getNewShape().getTarget(),
                newTarget.getType());
    }

    private static String createTraitDiffMessage(Shape oldShape, Shape newShape) {
        StringJoiner joiner = new StringJoiner(". ");
        ChangedShape<Shape> targetChange = new ChangedShape<>(oldShape, newShape);

        Set<ShapeId> removedTraits = targetChange.removedTraits()
                .map(Trait::toShapeId)
                .collect(Collectors.toCollection(TreeSet::new));

        if (!removedTraits.isEmpty()) {
            joiner.add("The targeted shape no longer has the following traits: " + removedTraits);
        }

        Set<ShapeId> addedTraits = targetChange.addedTraits()
                .map(Trait::toShapeId)
                .collect(Collectors.toCollection(TreeSet::new));

        if (!addedTraits.isEmpty()) {
            joiner.add("The newly targeted shape now has the following additional traits: " + addedTraits);
        }

        // Only select the traits that exist in both placed but changed.
        Set<ShapeId> changedTraits = new TreeSet<>(targetChange.getTraitDifferences().keySet());
        changedTraits.removeAll(addedTraits);
        changedTraits.removeAll(removedTraits);

        if (!changedTraits.isEmpty()) {
            joiner.add("The newly targeted shape has traits that differ from the previous shape: " + changedTraits);
        }

        return joiner.toString();
    }
}
