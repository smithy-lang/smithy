/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.HttpQueryParamsTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * When the `httpQueryParams` trait is used, this validator emits a NOTE when another member of the container shape
 * applies the `httpQuery` trait which may result in a conflict within the query string.
 */
public final class HttpQueryParamsTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        if (!model.isTraitApplied(HttpQueryParamsTrait.class)) {
            return Collections.emptyList();
        } else {
            return validateQueryTraitUsage(model);
        }
    }

    private List<ValidationEvent> validateQueryTraitUsage(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        for (MemberShape member : model.getMemberShapesWithTrait(HttpQueryParamsTrait.class)) {
            model.getShape(member.getContainer())
                    .flatMap(Shape::asStructureShape)
                    .ifPresent(structure -> {
                        // Gather the names of member shapes, as strings, that apply HttpQuery traits
                        List<String> queryShapes = getMembersWithTrait(structure, HttpQueryTrait.class);
                        if (queryShapes.size() > 0) {
                            events.add(createNote(structure, member.getMemberName(), queryShapes));
                        }
                    });
        }

        return events;
    }

    private List<String> getMembersWithTrait(StructureShape structure, Class<? extends Trait> trait) {
        List<String> members = new ArrayList<>();
        for (MemberShape member : structure.members()) {
            if (member.hasTrait(trait)) {
                members.add(member.getMemberName());
            }
        }
        return members;
    }

    private ValidationEvent createNote(Shape target, String queryParamsShape, List<String> queryShapes) {
        return note(target,
                String.format("Structure member `%s` is marked with the `httpQueryParams` trait, and "
                        + "`httpQuery` traits are applied to the following members: %s. The service will not be able to "
                        + "disambiguate between query string parameters intended for the `%s` member and those explicitly "
                        + "bound to the `httpQuery` members.",
                        queryParamsShape,
                        ValidationUtils.tickedList(queryShapes),
                        queryParamsShape));
    }
}
