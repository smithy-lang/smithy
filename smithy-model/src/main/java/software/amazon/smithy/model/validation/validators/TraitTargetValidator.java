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
        Map<Selector, Set<Shape>> selectorCache = new HashMap<>();

        // Only validate trait targets for traits that are actually used.
        for (ShapeId traitId : model.getAppliedTraits()) {
            model.getShape(traitId).ifPresent(traitShape -> {
                // Find all shapes that have the used trait applied to it.
                Set<Shape> shapes = model.getShapesWithTrait(traitId);
                validateMixinsUsedAsTraits(traitShape, shapes, events);
                traitShape.getTrait(TraitDefinition.class).ifPresent(definition -> {
                    validateTraitTargets(model, events, traitId, definition.getSelector(), shapes, selectorCache);
                });
            });
        }

        selectorCache.clear();
        return events;
    }

    private void validateMixinsUsedAsTraits(Shape traitShape, Set<Shape> appliedTo, List<ValidationEvent> events) {
        if (traitShape.hasTrait(MixinTrait.class)) {
            for (Shape shape : appliedTo) {
                events.add(error(shape, String.format(
                        "Trait `%s` is a mixin and cannot be applied to `%s`.",
                        Trait.getIdiomaticTraitName(traitShape.getId()),
                        shape.getId())));
            }
        }
    }

    private void validateTraitTargets(
            Model model,
            List<ValidationEvent> events,
            ShapeId trait,
            Selector selector,
            Set<Shape> appliedTo,
            Map<Selector, Set<Shape>> selectorCache
    ) {
        // Short circuit for shapes that match everything.
        if (selector.toString().equals("*")) {
            return;
        }

        // Find the shapes that this trait can be applied to.
        // Many selectors are identical to other selectors, so use a cache
        // to reduce the number of times the entire model is traversed.
        Set<Shape> matches = selectorCache.computeIfAbsent(selector, s -> s.select(model));

        for (Shape shape : appliedTo) {
            // Emit events when a shape is applied to something that didn't match the selector.
            if (!matches.contains(shape)) {
                // Strip out newlines with successive spaces.
                String sanitized = SANITIZE.matcher(selector.toString()).replaceAll(" ");
                events.add(error(shape, shape.findTrait(trait).get(), String.format(
                        "Trait `%s` cannot be applied to `%s`. This trait may only be applied "
                        + "to shapes that match the following selector: %s",
                        Trait.getIdiomaticTraitName(trait.toShapeId()),
                        shape.getId(),
                        sanitized)));
            }
        }
    }
}
