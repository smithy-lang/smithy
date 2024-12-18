/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.validators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.MixinTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Validates that traits are only applied to compatible shapes.
 *
 * <p>A shape must be present in the return value of a selector in order
 * for the shape to be considered compatible with the selector.
 *
 * <p>This validator emits ERROR events when a trait is applied to a shape
 * that is not compatible with the trait selector.
 */
public final class TraitTargetValidator extends AbstractValidator {

    private static final Pattern SANITIZE = Pattern.compile("\n\\s*");

    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();

        // Group shapes to validate by duplicate selectors to ensure that the
        // selector is only performed once.
        Map<Selector, List<ShapeId>> selectorsToTraits = new HashMap<>();

        // Only validate trait targets for traits that are actually used.
        for (ShapeId traitId : model.getAppliedTraits()) {
            model.getShape(traitId).ifPresent(traitShape -> {
                // Find all shapes that have the used trait applied to it.
                Set<Shape> shapes = model.getShapesWithTrait(traitId);
                validateMixinsUsedAsTraits(traitShape, shapes, events);
                traitShape.getTrait(TraitDefinition.class).ifPresent(definition -> {
                    // Short circuit for traits that match everything.
                    if (!definition.getSelector().toString().equals("*")) {
                        selectorsToTraits
                                .computeIfAbsent(definition.getSelector(), selector -> new ArrayList<>())
                                .add(traitId);
                    }
                });
            });
        }

        for (Map.Entry<Selector, List<ShapeId>> entry : selectorsToTraits.entrySet()) {
            validateTraitTargets(model, events, entry.getKey(), entry.getValue());
        }

        return events;
    }

    private void validateMixinsUsedAsTraits(Shape traitShape, Set<Shape> appliedTo, List<ValidationEvent> events) {
        if (traitShape.hasTrait(MixinTrait.class)) {
            for (Shape shape : appliedTo) {
                events.add(error(shape,
                        String.format(
                                "Trait `%s` is a mixin and cannot be applied to `%s`.",
                                Trait.getIdiomaticTraitName(traitShape.getId()),
                                shape.getId())));
            }
        }
    }

    private void validateTraitTargets(
            Model model,
            List<ValidationEvent> events,
            Selector selector,
            List<ShapeId> traits
    ) {
        Set<Shape> matches = selector.select(model);

        for (ShapeId traitId : traits) {
            // Find all shapes that have the used trait applied to it.
            for (Shape shape : model.getShapesWithTrait(traitId)) {
                // Emit events when a shape is applied to something that didn't match the selector.
                if (!matches.contains(shape)) {
                    // Strip out newlines with successive spaces.
                    String sanitized = SANITIZE.matcher(selector.toString()).replaceAll(" ");
                    events.add(error(shape,
                            shape.findTrait(traitId).get(),
                            String.format(
                                    "Trait `%s` cannot be applied to `%s`. This trait may only be applied "
                                            + "to shapes that match the following selector: %s",
                                    Trait.getIdiomaticTraitName(traitId),
                                    shape.getId(),
                                    sanitized)));
                }
            }
        }
    }
}
