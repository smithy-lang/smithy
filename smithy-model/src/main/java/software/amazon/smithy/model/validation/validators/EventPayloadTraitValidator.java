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

package software.amazon.smithy.model.validation.validators;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.EventHeaderTrait;
import software.amazon.smithy.model.traits.EventPayloadTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.FunctionalUtils;

/**
 * Ensures that when an event structure contains an eventPayload member,
 * that all other members are bound to headers.
 *
 * <p>Only a single member can be marked with the eventPayload trait, and
 * this is validated using {@link ExclusiveStructureMemberTraitValidator}.
 */
public final class EventPayloadTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes(StructureShape.class)
                .flatMap(this::validateEvent)
                .collect(Collectors.toList());
    }

    private Stream<ValidationEvent> validateEvent(StructureShape shape) {
        List<String> unmarked = shape.getAllMembers().values().stream()
                .filter(FunctionalUtils.not(this::isMarked))
                .map(MemberShape::getMemberName)
                .collect(Collectors.toList());
        boolean payloads = shape.getAllMembers().values().stream()
                .anyMatch(value -> value.getTrait(EventPayloadTrait.class).isPresent());

        if (unmarked.isEmpty() || !payloads) {
            return Stream.empty();
        }

        return Stream.of(error(shape, String.format(
                "This event structure contains a member marked with the `eventPayload` trait, so all other members "
                + "must be marked with the `eventHeader` trait. However, the following member(s) are not marked "
                + "with the eventHeader trait: %s", ValidationUtils.tickedList(unmarked))));
    }

    private boolean isMarked(Shape s) {
        return s.getTrait(EventHeaderTrait.class).isPresent() || s.getTrait(EventPayloadTrait.class).isPresent();
    }
}
