/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.HttpQueryTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.model.validation.ValidationUtils;

/**
 * Validates that httpQuery trait bindings are case-sensitively unique.
 */
public final class HttpQueryTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        if (!model.isTraitApplied(HttpQueryTrait.class)) {
            return Collections.emptyList();
        } else {
            return validateBindings(getQueryBindings(model));
        }
    }

    private Map<StructureShape, Map<String, Set<String>>> getQueryBindings(Model model) {
        Map<StructureShape, Map<String, Set<String>>> queryBindings = new HashMap<>();

        // Find all members in the model that have the HttpQuery trait.
        for (Shape shape : model.getShapesWithTrait(HttpQueryTrait.class)) {
            shape.asMemberShape().ifPresent(member -> {
                // Get the structure of the member. Validation events are going to be
                // applied to the structure and not to members.
                model.getShape(member.getContainer()).flatMap(Shape::asStructureShape).ifPresent(structure -> {
                    HttpQueryTrait trait = shape.expectTrait(HttpQueryTrait.class);
                    queryBindings
                            .computeIfAbsent(structure, s -> new HashMap<>())
                            .computeIfAbsent(trait.getValue(), v -> new HashSet<>())
                            .add(member.getMemberName());
                });
            });
        }

        return queryBindings;
    }

    private List<ValidationEvent> validateBindings(Map<StructureShape, Map<String, Set<String>>> queryBindings) {
        List<ValidationEvent> events = new ArrayList<>();

        for (Map.Entry<StructureShape, Map<String, Set<String>>> entry : queryBindings.entrySet()) {
            for (Map.Entry<String, Set<String>> paramsToMembers : entry.getValue().entrySet()) {
                // Emit if there are bindings on this shape for the same query string parameter.
                if (paramsToMembers.getValue().size() > 1) {
                    events.add(error(entry.getKey(), String.format(
                            "`httpQuery` parameter name binding conflicts found for the `%s` parameter in the "
                            + "following structure members: %s",
                            paramsToMembers.getKey(), ValidationUtils.tickedList(paramsToMembers.getValue()))));
                }
            }
        }

        return events;
    }
}
