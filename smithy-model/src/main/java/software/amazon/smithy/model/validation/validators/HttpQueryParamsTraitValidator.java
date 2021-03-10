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
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.HttpQueryParamsTrait;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

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

        for (Shape shape : model.getShapesWithTrait(HttpQueryParamsTrait.class)) {
            shape.asMemberShape().flatMap(member -> model.getShape(member.getContainer())
                .flatMap(Shape::asStructureShape))
                .ifPresent(structure -> {
                    for (MemberShape memberShape : structure.members()) {
                        if (memberShape.hasTrait(HttpQueryTrait.class)) {
                            events.add(note(shape, String.format("Trait `httpQueryParams` may potentially conflict with"
                                    + " `httpQuery` trait applied to `%s`.", memberShape.toShapeId())));
                        }
                    }
                });
        }

        return events;
    }
}
