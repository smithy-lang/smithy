/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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
        List<ValidationEvent> events = new ArrayList<>();
        for (MemberShape member : model.getMemberShapesWithTrait(EventPayloadTrait.class)) {
            model.getShape(member.getContainer())
                    .flatMap(Shape::asStructureShape)
                    .flatMap(structure -> validateEvent(structure, member))
                    .ifPresent(events::add);
        }

        return events;
    }

    private Optional<ValidationEvent> validateEvent(StructureShape shape, MemberShape payload) {
        List<String> unmarked = shape.getAllMembers()
                .values()
                .stream()
                .filter(FunctionalUtils.not(this::isMarked))
                .map(MemberShape::getMemberName)
                .collect(Collectors.toList());

        if (unmarked.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(error(shape,
                String.format(
                        "This event structure contains a member marked with the `eventPayload` trait, so all other members "
                                + "must be marked with the `eventHeader` trait. However, the following member(s) are not marked "
                                + "with the eventHeader trait: %s",
                        ValidationUtils.tickedList(unmarked))));
    }

    private boolean isMarked(Shape s) {
        return s.getTrait(EventHeaderTrait.class).isPresent() || s.getTrait(EventPayloadTrait.class).isPresent();
    }
}
