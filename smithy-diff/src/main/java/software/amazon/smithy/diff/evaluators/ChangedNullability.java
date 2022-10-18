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
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Stream;
import software.amazon.smithy.diff.ChangedShape;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NullableIndex;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
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

        List<ValidationEvent> events = new ArrayList<>();

        Stream<ChangedShape<MemberShape>> changed = Stream.concat(
            // Get members that changed.
            differences.changedShapes(MemberShape.class),
            // Get members of structures that added/removed the input trait.
            changedInputMembers(differences)
        );

        changed.map(change -> {
            // If NullableIndex says the nullability of a member changed, then that's a breaking change.
            MemberShape oldShape = change.getOldShape();
            MemberShape newShape = change.getNewShape();
            boolean wasNullable = oldIndex.isMemberNullable(oldShape);
            boolean isNowNullable = newIndex.isMemberNullable(newShape);
            return wasNullable == isNowNullable ? null : createError(differences, change, wasNullable);
        }).filter(Objects::nonNull).forEach(events::add);

        return events;
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

    private ValidationEvent createError(
            Differences differences,
            ChangedShape<MemberShape> change,
            boolean wasNullable
    ) {
        MemberShape oldMember = change.getOldShape();
        MemberShape newMember = change.getNewShape();
        String message = String.format("Member `%s` changed from %s to %s: ",
                                       oldMember.getMemberName(),
                                       wasNullable ? "nullable" : "non-nullable",
                                       wasNullable ? "non-nullable" : "nullable");
        StringJoiner joiner = new StringJoiner("; ", message, "");
        boolean oldHasInput = hasInputTrait(differences.getOldModel(), oldMember);
        boolean newHasInput = hasInputTrait(differences.getNewModel(), newMember);
        Shape newTarget = differences.getNewModel().expectShape(newMember.getTarget());
        Severity severity = null;

        if (oldHasInput && !newHasInput) {
            // If there was an input trait before, but not now, then the nullability must have
            // changed from nullable to non-nullable.
            joiner.add("The @input trait was removed from " + newMember.getContainer());
            severity = Severity.ERROR;
        } else if (!oldHasInput && newHasInput) {
            // If there was no input trait before, but there is now, then the nullability must have
            // changed from non-nullable to nullable.
            joiner.add("The @input trait was added to " + newMember.getContainer());
            severity = Severity.DANGER;
        } else if (!newHasInput) {
            // Can't add nullable to a preexisting required member.
            if (change.isTraitAdded(ClientOptionalTrait.ID) && change.isTraitInBoth(RequiredTrait.ID)) {
                joiner.add("The @nullable trait was added to a @required member.");
                severity = Severity.ERROR;
            }
            // Can't add required to a member unless the member is marked as nullable.
            if (change.isTraitAdded(RequiredTrait.ID) && !newMember.hasTrait(ClientOptionalTrait.ID)) {
                joiner.add("The @required trait was added to a member that is not marked as @nullable.");
                severity = Severity.ERROR;
            }
            // Can't add the default trait to a member unless the member was previously required.
            if (change.isTraitAdded(DefaultTrait.ID) && !change.isTraitRemoved(RequiredTrait.ID)) {
                joiner.add("The @default trait was added to a member that was not previously @required.");
                severity = Severity.ERROR;
            }
            // Can only remove the required trait if the member was nullable or replaced by the default trait.
            if (change.isTraitRemoved(RequiredTrait.ID)
                    && !newMember.hasTrait(DefaultTrait.ID)
                    && !oldMember.hasTrait(ClientOptionalTrait.ID)) {
                if (newTarget.isStructureShape() || newTarget.isUnionShape()) {
                    severity = severity == null ? Severity.WARNING : severity;
                    joiner.add("The @required trait was removed from a member that targets a " + newTarget.getType()
                               + ". This is backward compatible in generators that always treat structures and "
                               + "unions as optional (e.g., AWS generators)");
                } else {
                    joiner.add("The @required trait was removed and not replaced with the @default trait and "
                               + "@addedDefault trait.");
                    severity = Severity.ERROR;
                }
            }
        }

        // If no explicit severity was detected, then assume an error.
        severity = severity == null ? Severity.ERROR : severity;

        return ValidationEvent.builder()
                .id(getEventId())
                .shapeId(change.getNewShape())
                .severity(severity)
                .message(joiner.toString())
                .build();
    }

    private boolean hasInputTrait(Model model, MemberShape member) {
        return model.getShape(member.getContainer()).filter(shape -> shape.hasTrait(InputTrait.ID)).isPresent();
    }
}
