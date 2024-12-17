/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.neighbor.Relationship;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.traits.UnitTypeTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Ensures that a unit type can only be referenced as operation
 * input/output or by a tagged union member.
 */
public final class UnitTypeValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        NeighborProvider neighborProvider = NeighborProviderIndex.of(model).getReverseProvider();

        Shape unit = model.getShape(UnitTypeTrait.UNIT).orElse(null);
        if (unit == null) {
            return Collections.emptyList();
        }

        List<ValidationEvent> events = new ArrayList<>();
        for (Relationship relationship : neighborProvider.getNeighbors(unit)) {
            switch (relationship.getRelationshipType()) {
                case INPUT:
                case OUTPUT:
                    break;
                case MEMBER_TARGET:
                    relationship.getShape()
                            .asMemberShape()
                            .map(MemberShape::getContainer)
                            .flatMap(model::getShape)
                            .filter(shape -> !(shape.getType() == ShapeType.UNION
                                    || shape.getType() == ShapeType.ENUM
                                    || shape.getType() == ShapeType.INT_ENUM))
                            .ifPresent(container -> {
                                events.add(error(
                                        relationship.getShape(),
                                        "Only members of a union, enum, or intEnum can reference smithy.api#Unit"));
                            });
                    break;
                default:
                    events.add(error(relationship.getShape(),
                            String.format(
                                    "This shape has an invalid %s reference to smithy.api#Unit",
                                    relationship.getRelationshipType())));
            }
        }

        return events;
    }
}
