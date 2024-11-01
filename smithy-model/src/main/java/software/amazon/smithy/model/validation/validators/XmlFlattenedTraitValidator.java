/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.XmlFlattenedTrait;
import software.amazon.smithy.model.traits.XmlNameTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that xmlFlattened members aren't unintentionally ignoring the
 * xmlName of their targets.
 */
public final class XmlFlattenedTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (MemberShape member : model.getMemberShapesWithTrait(XmlFlattenedTrait.class)) {
            // Don't emit the event if they're being explicit about the xmlName on this member
            if (member.hasTrait(XmlNameTrait.class)) {
                continue;
            }

            model.getShape(member.getTarget())
                    .flatMap(Shape::asListShape)
                    .flatMap(listShape -> validateMemberTargetingList(member, listShape))
                    .ifPresent(events::add);
        }
        return events;
    }

    private Optional<ValidationEvent> validateMemberTargetingList(MemberShape member, ListShape targetList) {
        return targetList.getMember().getTrait(XmlNameTrait.class)
                .filter(xmlName -> !member.getMemberName().equals(xmlName.getValue()))
                .map(xmlName -> warning(member, String.format(
                        "Member is `@xmlFlattened`, so `@xmlName` of target's member (`%s`) has no effect."
                        + " The flattened list elements will have the name of this member - `%s`. If this"
                        + " is unintended, you can add `@xmlName(\"%s\")` to this member.",
                        targetList.getMember().getId(), member.getMemberName(), xmlName.getValue())));
    }
}
