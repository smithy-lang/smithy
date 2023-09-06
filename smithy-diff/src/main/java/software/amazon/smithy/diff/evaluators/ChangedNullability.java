/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import software.amazon.smithy.diff.ChangedShape;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NullableIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
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
public class ChangedNullability extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        NullableIndex oldIndex = NullableIndex.of(differences.getOldModel());
        NullableIndex newIndex = NullableIndex.of(differences.getNewModel());
        Set<ValidationEvent> events = new HashSet<>();

        Stream.concat(
            // Get members that changed.
            differences.changedShapes(MemberShape.class),
            // Get members of structures that added/removed the input trait.
            changedInputMembers(differences)
        ).forEach(change -> {
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
                .flatMap(change -> change.getNewShape().members().stream()
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
        String message = String.format("Member `%s` changed from %s to %s: ",
                                       oldMember.getMemberName(),
                                       wasNullable ? "nullable" : "non-nullable",
                                       wasNullable ? "non-nullable" : "nullable");
        boolean oldHasInput = hasInputTrait(differences.getOldModel(), oldMember);
        boolean newHasInput = hasInputTrait(differences.getNewModel(), newMember);
        ShapeId shape = change.getShapeId();
        Shape newTarget = differences.getNewModel().expectShape(newMember.getTarget());
        List<ValidationEvent> eventsToAdd = new ArrayList<>();

        if (oldHasInput && !newHasInput) {
            // If there was an input trait before, but not now, then the nullability must have
            // changed from nullable to non-nullable.
            eventsToAdd.add(emit(Severity.ERROR, "RemovedInputTrait", shape, message,
                            "The @input trait was removed from " + newMember.getContainer()));
        } else if (!oldHasInput && newHasInput) {
            // If there was no input trait before, but there is now, then the nullability must have
            // changed from non-nullable to nullable.
            eventsToAdd.add(emit(Severity.DANGER, "AddedInputTrait", shape, message,
                            "The @input trait was added to " + newMember.getContainer()));
        } else if (!newHasInput) {
            // Can't add clientOptional to a preexisting required member.
            if (change.isTraitAdded(ClientOptionalTrait.ID) && change.isTraitInBoth(RequiredTrait.ID)) {
                eventsToAdd.add(emit(Severity.ERROR, "AddedClientOptionalTrait", shape, message,
                                "The @clientOptional trait was added to a @required member."));
            }
            // Can't add required to a member unless the member is marked as @clientOptional or part of @input.
            if (change.isTraitAdded(RequiredTrait.ID) && !newMember.hasTrait(ClientOptionalTrait.ID)) {
                eventsToAdd.add(emit(Severity.ERROR, "AddedRequiredTrait", shape, message,
                                "The @required trait was added to a member."));
            }
            // Can't add the default trait to a member unless the member was previously required.
            if (change.isTraitAdded(DefaultTrait.ID) && !change.isTraitRemoved(RequiredTrait.ID)) {
                eventsToAdd.add(emit(Severity.ERROR, "AddedDefaultTrait", shape, message,
                                "The @default trait was added to a member that was not previously @required."));
            }
            // Can only remove the required trait if the member was nullable or replaced by the default trait.
            if (change.isTraitRemoved(RequiredTrait.ID)
                    && !newMember.hasTrait(DefaultTrait.ID)
                    && !oldMember.hasTrait(ClientOptionalTrait.ID)) {
                if (newTarget.isStructureShape() || newTarget.isUnionShape()) {
                    eventsToAdd.add(emit(Severity.WARNING, "RemovedRequiredTrait.StructureOrUnion", shape,
                                    message, "The @required trait was removed from a member that targets a "
                                    + newTarget.getType() + ". This is backward compatible in generators that "
                                    + "always treat structures and unions as optional (e.g., AWS generators)"));
                } else {
                    eventsToAdd.add(emit(Severity.ERROR, "RemovedRequiredTrait", shape, message,
                                    "The @required trait was removed and not replaced with the @default trait and "
                                    + "@addedDefault trait."));
                }
            }
        }

        // If not specific event was emitted, emit a generic event.
        if (eventsToAdd.isEmpty()) {
            eventsToAdd.add(emit(Severity.ERROR, null, shape, null, message));
        }

        events.addAll(eventsToAdd);
    }

    private boolean hasInputTrait(Model model, MemberShape member) {
        return model.getShape(member.getContainer()).filter(shape -> shape.hasTrait(InputTrait.ID)).isPresent();
    }

    private ValidationEvent emit(
            Severity severity,
            String eventIdSuffix,
            ShapeId shape,
            String prefixMessage,
            String message
    ) {
        String actualId = eventIdSuffix == null ? getEventId() : (getEventId() + '.' + eventIdSuffix);
        String actualMessage = prefixMessage == null ? message : (prefixMessage + "; " + message);
        return ValidationEvent.builder()
                .id(actualId)
                .shapeId(shape)
                .message(actualMessage)
                .severity(severity)
                .build();
    }
}
