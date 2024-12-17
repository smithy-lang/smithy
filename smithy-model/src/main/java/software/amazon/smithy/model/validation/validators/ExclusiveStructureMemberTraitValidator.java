/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * Validates traits that can only be applied to a single structure member.
 */
public final class ExclusiveStructureMemberTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        // Find all traits that are exclusive by member and target.
        Set<ShapeId> exclusiveMemberTraits = new HashSet<>();
        Set<ShapeId> exclusiveTargetTraits = new HashSet<>();

        for (Shape shape : model.getShapesWithTrait(TraitDefinition.class)) {
            TraitDefinition definition = shape.expectTrait(TraitDefinition.class);
            if (definition.isStructurallyExclusiveByTarget()) {
                exclusiveTargetTraits.add(shape.getId());
            } else if (definition.isStructurallyExclusiveByMember()) {
                exclusiveMemberTraits.add(shape.getId());
            }
        }

        List<ValidationEvent> events = new ArrayList<>();
        for (Shape shape : model.getStructureShapes()) {
            validateExclusiveMembers(shape, exclusiveMemberTraits, events);
            validateExclusiveTargets(model, shape, exclusiveTargetTraits, events);
        }

        return events;
    }

    private void validateExclusiveMembers(
            Shape shape,
            Set<ShapeId> exclusiveMemberTraits,
            List<ValidationEvent> events
    ) {
        for (ShapeId traitId : exclusiveMemberTraits) {
            List<String> matches = new ArrayList<>();
            for (MemberShape member : shape.members()) {
                if (member.findTrait(traitId).isPresent()) {
                    matches.add(member.getMemberName());
                }
            }

            if (matches.size() > 1) {
                events.add(error(shape,
                        String.format(
                                "The `%s` trait can be applied to only a single member of a shape, but it was found on "
                                        + "the following members: %s",
                                Trait.getIdiomaticTraitName(traitId),
                                ValidationUtils.tickedList(matches))));
            }
        }
    }

    private void validateExclusiveTargets(
            Model model,
            Shape shape,
            Set<ShapeId> exclusiveTargets,
            List<ValidationEvent> events
    ) {
        // Find all member targets that violate the exclusion rule (e.g., streaming trait).
        for (ShapeId id : exclusiveTargets) {
            List<String> matches = new ArrayList<>();
            for (MemberShape member : shape.members()) {
                if (memberTargetHasTrait(model, member, id)) {
                    matches.add(member.getMemberName());
                }
            }

            if (matches.size() > 1) {
                events.add(error(shape,
                        String.format(
                                "Only a single member of a structure can target a shape marked with the `%s` trait, "
                                        + "but it was found on the following members: %s",
                                Trait.getIdiomaticTraitName(id),
                                ValidationUtils.tickedList(matches))));
            }
        }
    }

    private boolean memberTargetHasTrait(Model model, MemberShape member, ShapeId trait) {
        return model.getShape(member.getTarget())
                .flatMap(target -> target.findTrait(trait))
                .isPresent();
    }
}
