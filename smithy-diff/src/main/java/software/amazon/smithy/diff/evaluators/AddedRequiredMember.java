/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.stream.Stream;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.DefaultTrait;
import software.amazon.smithy.model.traits.RequiredTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that no members are newly created with the required trait
 * (but no default trait) in existing structures.
 */
public class AddedRequiredMember extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        List<ValidationEvent> events = newRequiredMembers(differences)
                .map(this::emit)
                .collect(Collectors.toList());

        return events;
    }

    private Stream<MemberShape> newRequiredMembers(Differences differences) {
        return differences.changedShapes(StructureShape.class)
                .flatMap(change -> change.getNewShape().members().stream()
                        .filter(newMember -> newMember.hasTrait(RequiredTrait.ID)
                                && !newMember.hasTrait(DefaultTrait.ID)
                                // Members that did not exist before
                                && change.getOldShape().getAllMembers().get(newMember.getMemberName()) == null));
    }

    private ValidationEvent emit(MemberShape memberShape) {
        return ValidationEvent.builder()
                .id(getEventId())
                .shape(memberShape)
                .message("Adding a new member with the `required` trait "
                        + "but not the `default` trait is backwards-incompatible.")
                .severity(Severity.ERROR)
                .build();
    }
}
