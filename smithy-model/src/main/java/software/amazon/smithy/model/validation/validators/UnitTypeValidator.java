/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
                    events.add(error(relationship.getShape(), String.format(
                            "This shape has an invalid %s reference to smithy.api#Unit",
                            relationship.getRelationshipType())));
            }
        }

        return events;
    }
}
