/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.loader.Prelude;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.DeprecatedTrait;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.SetUtils;

/**
 * Emits a validation event if a model contains shapes that are bound to deprecated traits.
 */
public final class DeprecatedTraitValidator extends AbstractValidator {

    // The set of trait shape IDs where deprecation warnings are emitted elsewhere.
    // For example, enum trait deprecation warnings are only emitted when loading 2.0 models.
    private static final Set<ShapeId> HANDLED_ELSEWHERE = SetUtils.of(EnumTrait.ID);

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        for (Shape trait : model.getShapesWithTrait(TraitDefinition.class)) {
            if (trait.hasTrait(DeprecatedTrait.class)) {
                // Don't emit for deprecation warnings that are handled by some other validation.
                if (HANDLED_ELSEWHERE.contains(trait.toShapeId())) {
                    continue;
                }
                Set<Shape> shapesWithTrait = model.getShapesWithTrait(trait);
                if (!shapesWithTrait.isEmpty()) {
                    DeprecatedTrait deprecatedTrait = trait.expectTrait(DeprecatedTrait.class);
                    String traitMessage = trait.toShapeId().toString();
                    if (deprecatedTrait.getMessage().isPresent()) {
                        traitMessage = traitMessage + ", " + deprecatedTrait.getMessage().get();
                    }
                    for (Shape shape : shapesWithTrait) {
                        // Ignore the use of @box on prelude shapes.
                        if (!Prelude.isPreludeShape(shape)) {
                            events.add(warning(shape,
                                    shape.findTrait(trait.getId()).get(),
                                    format(
                                            "This shape applies a trait that is deprecated: %s",
                                            traitMessage)));
                        }
                    }
                }
            }
        }

        return events;
    }
}
