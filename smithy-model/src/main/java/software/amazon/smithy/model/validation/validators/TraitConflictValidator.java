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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that traits do not conflict.
 */
public final class TraitConflictValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes()
                .flatMap(shape -> {
                    // Map of trait shape IDs to trait value.
                    Map<ShapeId, Trait> traits = shape.getAllTraits();
                    // Map of trait shape ID to a list of found conflicting traits.
                    Map<ShapeId, List<ShapeId>> conflicts = new HashMap<>();

                    traits.forEach((k, v) -> {
                        model.getTraitDefinition(v.toShapeId()).ifPresent(definition -> {
                            definition.getConflicts().forEach(conflict -> {
                                if (traits.containsKey(conflict)) {
                                    conflicts.computeIfAbsent(k, key -> new ArrayList<>()).add(conflict);
                                }
                            });
                        });
                    });

                    return conflicts.isEmpty() ? Stream.empty() : Stream.of(foundConflicts(shape, conflicts));
                })
                .collect(Collectors.toList());
    }

    private ValidationEvent foundConflicts(Shape shape, Map<ShapeId, List<ShapeId>> conflicts) {
        return error(shape, "Found conflicting traits on " + shape.getType() + " shape: "
                            + conflicts.entrySet().stream()
                                    .flatMap(this::lines)
                                    .sorted()
                                    .collect(Collectors.joining(", ")));
    }

    private Stream<String> lines(Map.Entry<ShapeId, List<ShapeId>> entry) {
        String prefix = "`" + Trait.getIdiomaticTraitName(entry.getKey()) + "` conflicts with `";
        return entry.getValue().stream().map(value -> prefix + Trait.getIdiomaticTraitName(value) + "`");
    }
}
