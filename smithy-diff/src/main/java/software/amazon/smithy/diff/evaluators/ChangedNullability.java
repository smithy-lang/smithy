/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import software.amazon.smithy.diff.ChangedShape;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.knowledge.NullableIndex;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ClientOptionalTrait;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.InputTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that only backward compatible changes are made to
 * structure member nullability to ensure that if something was
 * previously nullable to clients then it continue to be nullable
 * and vice versa.
 */
public final class ChangedNullability extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        NullableIndex oldIndex = NullableIndex.of(differences.getOldModel());
        NullableIndex newIndex = NullableIndex.of(differences.getNewModel());
        Set<ValidationEvent> events = new HashSet<>();

        Stream.concat(
                // Get members that changed.
                differences.changedShapes(MemberShape.class),
                // Get members of structures that added/removed the input trait.
                changedInputMembers(differences)).forEach(change -> {
                    // If NullableIndex says the nullability of a member changed, then that's a breaking change.
                    MemberShape oldShape = change.getOldShape();
                    MemberShape newShape = change.getNewShape();
                    boolean wasNullable = oldIndex.isMemberNullable(oldShape);
                    boolean isNowNullable = newIndex.isMemberNullable(newShape);
                    if (wasNullable != isNowNullable) {
                        createErrors(differences, change, wasNullable, events);
                    }
                });

        return new ArrayList<>(events);
    }

    private Stream<ChangedShape<MemberShape>> changedInputMembers(Differences differences) {
        return differences.changedShapes(StructureShape.class)
                .filter(change -> change.isTraitAdded(InputTrait.ID) || change.isTraitRemoved(InputTrait.ID))
                // Find all members that existed before and exist now.
                .flatMap(change -> change.getNewShape()
                        .members()
                        .stream()
                        .map(newMember -> {
                            MemberShape old = change.getOldShape().getAllMembers().get(newMember.getMemberName());
                            return old == null ? null : new ChangedShape<>(old, newMember);
                        })
                        .filter(Objects::nonNull));
    }

    private void createErrors(
            Differences differences,
            ChangedShape<MemberShape> change,
            boolean wasNullable,
            Collection<ValidationEvent> events
    ) {
        MemberShape oldMember = change.getOldShape();
        MemberShape newMember = change.getNewShape();
        String message = String.format("Member `%s` changed from %s to %s",
                oldMember.getMemberName(),
                wasNullable ? "nullable" : "non-nullable",
                wasNullable ? "non-nullable" : "nullable");
        ShapeId shape = change.getShapeId();
        SourceLocation oldMemberSourceLocation = oldMember.getSourceLocation();
        Shape newTarget = differences.getNewModel().expectShape(newMember.getTarget());
        List<ValidationEvent> eventsToAdd = new ArrayList<>();
        SourceLocation newMemberContainerSource = differences.getNewModel()
                .expectShape(newMember.getContainer())
                .getSourceLocation();

        boolean oldHasInput = hasInputTrait(differences.getOldModel(), oldMember);
        boolean newHasInput = hasInputTrait(differences.getNewModel(), newMember);
        if (oldHasInput && !newHasInput) {
            // If there was an input trait before, but not now, then the nullability must have
            // changed from nullable to non-nullable.
            addRemovedInputTrait(eventsToAdd, shape, newMemberContainerSource, message, newMember.getContainer());
        } else if (!oldHasInput && newHasInput) {
            // If there was no input trait before, but there is now, then the nullability must have
            // changed from non-nullable to nullable.
            addAddedInputTrait(eventsToAdd, shape, newMemberContainerSource, message, newMember.getContainer());
        } else if (!newHasInput && !oldHasInput) {
            // Note, the condition '!oldHasInput' above is always 'true' when reached, but keeping
            // to make it clear the expectations below.
            if (change.isTraitAdded(ClientOptionalTrait.ID) && change.isTraitInBoth(RequiredTrait.ID)) {
                // Can't add clientOptional to a preexisting required member.
                addAddedClientOptionalTrait(eventsToAdd, shape, oldMemberSourceLocation, message);
            } else if (change.isTraitRemoved(ClientOptionalTrait.ID) && change.isTraitInBoth(RequiredTrait.ID)) {
                // Can't remove clientOptional from a preexisting required member.
                addRemovedClientOptionalTrait(eventsToAdd, shape, oldMemberSourceLocation, message);
            } else if (change.isTraitAdded(RequiredTrait.ID) && !newMember.hasTrait(ClientOptionalTrait.ID)) {
                if (newTarget.isStructureShape() || newTarget.isUnionShape()) {
                    // Adding required to a member that targets a structure or a union is backwards compatible for
                    // AWS code generators, just add a warning.
                    ShapeType type = newTarget.getType();
                    addAddedRequiredTraitStructureOrUnion(eventsToAdd, shape, oldMemberSourceLocation, message, type);
                } else {
                    // Can't add required to a member unless the member is marked as @clientOptional or part of @input.
                    addAddedRequiredTrait(eventsToAdd, shape, oldMemberSourceLocation, message);
                }
            }
            // Can't add the default trait to a member unless the member was previously required.
            if (change.isTraitAdded(DefaultTrait.ID) && !change.isTraitRemoved(RequiredTrait.ID)) {
                addAddedDefaultTrait(eventsToAdd, shape, oldMemberSourceLocation, message);
            }
            if (change.isTraitRemoved(RequiredTrait.ID) && !newMember.hasTrait(DefaultTrait.ID)
                    && !oldMember.hasTrait(ClientOptionalTrait.ID)) {
                if (newTarget.isStructureShape() || newTarget.isUnionShape()) {
                    // Removing the required trait if the member targets a structure or a union is backwards compatible
                    // for AWS code generators, just add a warning.
                    ShapeType type = newTarget.getType();
                    addRemovedRequiredTraitStructureOrUnion(eventsToAdd, shape, oldMemberSourceLocation, message, type);
                } else {
                    // Can only remove the required trait if the member was nullable or replaced by the default trait.
                    addRemovedRequiredTrait(eventsToAdd, shape, oldMemberSourceLocation, message);
                }
            }
        }
        if (change.isTraitInBoth(DefaultTrait.ID)) {
            Node oldValue = oldMember.getTrait(DefaultTrait.class).get().toNode();
            Node newValue = newMember.getTrait(DefaultTrait.class).get().toNode();
            boolean isOldDefaultNullable = oldValue.isNullNode();
            boolean isNewDefaultNullable = newValue.isNullNode();
            if (!isOldDefaultNullable && isNewDefaultNullable) {
                // Can't change a default trait from a non-null to a null value
                addAddedNullDefault(eventsToAdd, shape, oldMemberSourceLocation, message, oldValue);
            } else if (isOldDefaultNullable && !isNewDefaultNullable) {
                // Can't change a default trait from a null to a non-null value
                addRemovedNullDefault(eventsToAdd, shape, oldMemberSourceLocation, message, newValue);
            }
        }

        // If not specific event was emitted, emit a generic event.
        if (eventsToAdd.isEmpty()) {
            eventsToAdd.add(emit(Severity.ERROR, null, shape, oldMemberSourceLocation, null, message));
        }

        events.addAll(eventsToAdd);
    }

    private void addRemovedInputTrait(
            List<ValidationEvent> eventsToAdd,
            ShapeId shape,
            SourceLocation location,
            String message,
            ShapeId container
    ) {
        eventsToAdd.add(emit(Severity.ERROR,
                "RemovedInputTrait",
                shape,
                location,
                message,
                "The `@input` trait was removed from " + container));
    }

    private void addAddedInputTrait(
            List<ValidationEvent> eventsToAdd,
            ShapeId shape,
            SourceLocation location,
            String message,
            ShapeId container
    ) {
        eventsToAdd.add(emit(Severity.DANGER,
                "AddedInputTrait",
                shape,
                location,
                message,
                "The `@input` trait was added to " + container));
    }

    private void addAddedClientOptionalTrait(
            List<ValidationEvent> eventsToAdd,
            ShapeId shape,
            SourceLocation location,
            String message
    ) {
        eventsToAdd.add(emit(Severity.ERROR,
                "AddedClientOptionalTrait",
                shape,
                location,
                message,
                "The `@clientOptional` trait was added to a `@required` member."));
    }

    private void addRemovedClientOptionalTrait(
            List<ValidationEvent> eventsToAdd,
            ShapeId shape,
            SourceLocation location,
            String message
    ) {
        eventsToAdd.add(emit(Severity.ERROR,
                "RemovedClientOptionalTrait",
                shape,
                location,
                message,
                "The `@clientOptional` trait was removed from a `@required` member."));
    }

    private void addAddedRequiredTraitStructureOrUnion(
            List<ValidationEvent> eventsToAdd,
            ShapeId shape,
            SourceLocation location,
            String message,
            ShapeType targetType
    ) {
        eventsToAdd.add(emit(Severity.WARNING,
                "AddedRequiredTrait.StructureOrUnion",
                shape,
                location,
                message,
                "The `@required` trait was added to a member "
                        + "that targets a " + targetType + ". This is backward compatible in "
                        + "generators that always treat structures and unions as optional "
                        + "(e.g., AWS generators)"));
    }

    private void addAddedRequiredTrait(
            List<ValidationEvent> eventsToAdd,
            ShapeId shape,
            SourceLocation location,
            String message
    ) {
        eventsToAdd.add(emit(Severity.ERROR,
                "AddedRequiredTrait",
                shape,
                location,
                message,
                "The `@required` trait was added to a member."));
    }

    private void addAddedDefaultTrait(
            List<ValidationEvent> eventsToAdd,
            ShapeId shape,
            SourceLocation location,
            String message
    ) {
        eventsToAdd.add(emit(Severity.ERROR,
                "AddedDefaultTrait",
                shape,
                location,
                message,
                "The `@default` trait was added to a member that was not previously `@required`."));

    }

    private void addRemovedRequiredTraitStructureOrUnion(
            List<ValidationEvent> eventsToAdd,
            ShapeId shape,
            SourceLocation location,
            String message,
            ShapeType targetType
    ) {
        eventsToAdd.add(emit(Severity.WARNING,
                "RemovedRequiredTrait.StructureOrUnion",
                shape,
                location,
                message,
                "The @required trait was removed from a member "
                        + "that targets a " + targetType + ". This is backward compatible in "
                        + "generators that always treat structures and unions as optional "
                        + "(e.g., AWS generators)"));
    }

    private void addRemovedRequiredTrait(
            List<ValidationEvent> eventsToAdd,
            ShapeId shape,
            SourceLocation location,
            String message
    ) {
        eventsToAdd.add(emit(Severity.ERROR,
                "RemovedRequiredTrait",
                shape,
                location,
                message,
                "The @required trait was removed and not replaced with the @default "
                        + "trait and @addedDefault trait."));
    }

    private void addAddedNullDefault(
            List<ValidationEvent> eventsToAdd,
            ShapeId shape,
            SourceLocation location,
            String message,
            Node value
    ) {
        eventsToAdd.add(emit(Severity.ERROR,
                "AddedNullDefault",
                shape,
                location,
                message,
                "The `@default` trait was changed from `" + value + "` to `null`."));
    }

    private void addRemovedNullDefault(
            List<ValidationEvent> eventsToAdd,
            ShapeId shape,
            SourceLocation location,
            String message,
            Node value
    ) {
        eventsToAdd.add(emit(Severity.ERROR,
                "RemovedNullDefault",
                shape,
                location,
                message,
                "The `@default` trait was changed from `null` to `" + value + "`."));
    }

    private boolean hasInputTrait(Model model, MemberShape member) {
        return model.getShape(member.getContainer()).filter(shape -> shape.hasTrait(InputTrait.ID)).isPresent();
    }

    private ValidationEvent emit(
            Severity severity,
            String eventIdSuffix,
            ShapeId shape,
            SourceLocation sourceLocation,
            String prefixMessage,
            String message
    ) {
        String actualId = eventIdSuffix == null ? getEventId() : (getEventId() + '.' + eventIdSuffix);
        String actualMessage = prefixMessage == null ? message : (prefixMessage + ": " + message);
        return ValidationEvent.builder()
                .id(actualId)
                .shapeId(shape)
                .sourceLocation(sourceLocation)
                .message(actualMessage)
                .severity(severity)
                .build();
    }
}
