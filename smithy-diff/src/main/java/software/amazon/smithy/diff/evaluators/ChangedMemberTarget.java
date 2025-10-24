/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.diff.ChangedShape;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.diff.ModelDiff;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.CollectionShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.SimpleShape;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SetUtils;
import software.amazon.smithy.utils.StringUtils;

/**
 * Checks for changes in the shapes targeted by a member.
 *
 * <p>If the new target is not a compatible type, the emitted event will be
 * an ERROR. The new target is not compatible if any of the following are true:
 *
 * <ul>
 *     <li>The new target is a different shape type than the old target.</li>
 *     <li>The target is a shape type whose name is significant to code generation,
 *     such as structures and enums.</li>
 *     <li>The new target is a list whose member is not a compatible type with the
 *     old target's member.</li>
 *     <li>The new target is a map whose key or value is not a compatible type with
 *     the old target's key or value.</li>
 * </ul>
 *
 * <p>If the types are compatible, the emitted event will default to a WARNING. This
 * is elevated if any trait changes would result in a higher severity.
 */
public final class ChangedMemberTarget extends AbstractDiffEvaluator {

    /**
     * These traits are traits that are known to significantly influence
     * code generation. Right now it just contains the enum trait, but
     * other traits could be added in the future as needed.
     */
    private static final Set<ShapeId> SIGNIFICANT_CODEGEN_TRAITS = SetUtils.of(EnumTrait.ID);

    private static final Pair<Severity, String> COMPATIBLE =
            Pair.of(Severity.WARNING, "This was determined backward compatible.");

    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return evaluate(ChangedMemberTarget.class.getClassLoader(), differences);
    }

    @Override
    public List<ValidationEvent> evaluate(ClassLoader classLoader, Differences differences) {
        return differences.changedShapes(MemberShape.class)
                .filter(change -> !change.getOldShape().getTarget().equals(change.getNewShape().getTarget()))
                .map(change -> createChangeEvent(classLoader, differences, change))
                .collect(Collectors.toList());
    }

    private ValidationEvent createChangeEvent(
            ClassLoader classLoader,
            Differences differences,
            ChangedShape<MemberShape> change
    ) {
        return createChangeEvent(classLoader, differences.getOldModel(), differences.getNewModel(), change);
    }

    private ValidationEvent createChangeEvent(
            ClassLoader classLoader,
            Model oldModel,
            Model newModel,
            ChangedShape<MemberShape> change
    ) {
        Shape oldTarget = getShapeTarget(oldModel, change.getOldShape().getTarget());
        Shape newTarget = getShapeTarget(newModel, change.getNewShape().getTarget());

        Pair<Severity, String> evaluation = evaluateShape(
                classLoader,
                oldModel,
                newModel,
                oldTarget,
                newTarget);
        String message = createSimpleMessage(change, oldTarget, newTarget) + evaluation.getRight();

        return ValidationEvent.builder()
                .severity(evaluation.getLeft())
                .id(getEventId())
                .shape(change.getNewShape())
                .message(message)
                .build();
    }

    private Shape getShapeTarget(Model model, ShapeId id) {
        return model.getShape(id).orElse(null);
    }

    private Pair<Severity, String> evaluateShape(
            ClassLoader classLoader,
            Model oldModel,
            Model newModel,
            Shape oldShape,
            Shape newShape
    ) {
        if (oldShape == null || newShape == null) {
            return COMPATIBLE;
        }

        if (oldShape.getType() != newShape.getType()) {
            return Pair.of(
                    Severity.ERROR,
                    String.format(
                            "The type of the targeted shape changed from %s to %s.",
                            oldShape.getType(),
                            newShape.getType()));
        }

        if (!(oldShape instanceof SimpleShape || oldShape instanceof CollectionShape || oldShape.isMapShape())
                || oldShape.isIntEnumShape()
                || oldShape.isEnumShape()) {
            return Pair.of(
                    Severity.ERROR,
                    String.format("The name of a %s is significant.", oldShape.getType()));
        }

        for (ShapeId significantCodegenTrait : SIGNIFICANT_CODEGEN_TRAITS) {
            if (oldShape.hasTrait(significantCodegenTrait)) {
                return Pair.of(
                        Severity.ERROR,
                        String.format("The `%s` trait was found on the target, so the name of the targeted "
                                + "shape matters for codegen.",
                                significantCodegenTrait));
            }
        }

        // Now that we've checked several terminal conditions, we need to evaluate traits and
        // collection/map member targets. To evaluate traits, we will create a synthetic diff
        // set to re-run the diff evaluator on. That will ensure that any differences are
        // given the proper severity and context rather than simply returning an ERROR for any
        // difference.
        Differences.Builder differences = Differences.builder()
                .oldModel(oldModel)
                .newModel(newModel)
                .changedShape(new ChangedShape<>(oldShape, newShape));

        // Add any list / map members to the set of differences to check, and potentially
        // recurse if this evaluator needs to be run on them. Note that this can't recurse
        // infinitely, even without any specific checks here. That's because to get to this
        // point a member target had to change without changing shape type and without being
        // a structure, union, or enum. Neither maps nor lists can recurse by themselves or
        // with each other, there MUST be a structure or union in the path for recursion to
        // happen in a way that Smithy will allow. Therefore, when the structure or union
        // in the path is hit, it'll get caught in the terminal conditions above.
        if (oldShape instanceof CollectionShape) {
            MemberShape oldMember = ((CollectionShape) oldShape).getMember();
            MemberShape newMember = ((CollectionShape) newShape).getMember();
            differences.changedShape(new ChangedShape<>(oldMember, newMember));
        } else if (oldShape instanceof MapShape) {
            MapShape oldMap = (MapShape) oldShape;
            MapShape newMap = (MapShape) newShape;
            differences.changedShape(new ChangedShape<>(oldMap.getKey(), newMap.getKey()));
            differences.changedShape(new ChangedShape<>(oldMap.getValue(), newMap.getValue()));
        }

        // Re-run the diff evaluator with this changed shape and any changed members.
        ModelDiff.Result result = ModelDiff.builder()
                .oldModel(oldModel)
                .newModel(newModel)
                .classLoader(classLoader)
                .compare(differences.build());
        List<ValidationEvent> diffEvents = new ArrayList<>(result.getDiffEvents());

        if (diffEvents.isEmpty()) {
            return COMPATIBLE;
        }

        Severity severity = Severity.WARNING;
        StringBuilder message = new StringBuilder("This will result in the following effective differences:")
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        for (ValidationEvent event : diffEvents) {
            // If the severity in any event is greater than the current severity, elevate it
            // to that level.
            severity = severity.compareTo(event.getSeverity()) > 0 ? severity : event.getSeverity();

            // Add the event to a list and indent the message in case it also spans
            // multiple lines.
            String eventMessage = StringUtils.indent(event.getMessage(), 2).trim();
            message.append(String.format("- [%s] %s%n", event.getSeverity(), eventMessage));
        }

        // If there are only warnings or less,
        if (severity.compareTo(Severity.WARNING) <= 0) {
            message.insert(0, "This was determined backward compatible. ");
        }

        return Pair.of(severity, message.toString().trim());
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
}
