/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that traits do not conflict.
 */
public final class TraitConflictValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        // Find all trait definitions and collect conflicting traits into a map.
        Map<Shape, Map<ShapeId, List<ShapeId>>> shapeToTraitConflicts = new HashMap<>();
        for (Shape shape : model.getShapesWithTrait(TraitDefinition.class)) {
            TraitDefinition trait = shape.expectTrait(TraitDefinition.class);
            // Only look at trait definitions that define conflicting traits.
            if (!trait.getConflicts().isEmpty()) {
                findAndCollectConflicts(model, shape.getId(), trait.getConflicts(), shapeToTraitConflicts);
            }
        }

        if (shapeToTraitConflicts.isEmpty()) {
            return Collections.emptyList();
        }

        List<ValidationEvent> events = new ArrayList<>();
        for (Map.Entry<Shape, Map<ShapeId, List<ShapeId>>> entry : shapeToTraitConflicts.entrySet()) {
            events.add(emitForConflicts(entry.getKey(), entry.getValue()));
        }

        return events;
    }

    // Find shapes that use this trait and also apply conflicting traits.
    private void findAndCollectConflicts(
            Model model,
            ShapeId trait,
            List<ShapeId> conflicts,
            Map<Shape, Map<ShapeId, List<ShapeId>>> shapeToTraitConflicts
    ) {
        for (Shape shape : model.getShapesWithTrait(trait)) {
            for (ShapeId conflict : conflicts) {
                if (shape.hasTrait(conflict)) {
                    shapeToTraitConflicts
                            .computeIfAbsent(shape, id -> new HashMap<>())
                            .computeIfAbsent(trait, id -> new ArrayList<>())
                            .add(conflict);
                }
            }
        }
    }

    private ValidationEvent emitForConflicts(Shape shape, Map<ShapeId, List<ShapeId>> conflicts) {
        return error(shape,
                "Found conflicting traits on " + shape.getType() + " shape: "
                        + conflicts.entrySet()
                                .stream()
                                .flatMap(this::lines)
                                .sorted()
                                .collect(Collectors.joining(", ")));
    }

    private Stream<String> lines(Map.Entry<ShapeId, List<ShapeId>> entry) {
        String prefix = "`" + Trait.getIdiomaticTraitName(entry.getKey()) + "` conflicts with `";
        return entry.getValue().stream().map(value -> prefix + Trait.getIdiomaticTraitName(value) + "`");
    }
}
