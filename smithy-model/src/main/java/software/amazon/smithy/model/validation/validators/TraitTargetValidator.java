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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
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
    @Override
    public List<ValidationEvent> validate(Model model) {
        Collection<SelectorTest> tests = createTests(model);
        List<ValidationEvent> events = new ArrayList<>();
        NeighborProvider neighborProvider = model.getKnowledge(NeighborProviderIndex.class).getProvider();

        for (SelectorTest test : tests) {
            // Find the shapes that this trait can be applied to.
            Set<Shape> matches = test.selector.runner()
                    .model(model)
                    .neighborProvider(neighborProvider)
                    .selectShapes();

            // Remove the allowed locations from the real locations, leaving only
            // the shapes in the set that are invalid.
            test.appliedTo.removeAll(matches);

            for (Shape shape : test.appliedTo) {
                events.add(error(shape, test.trait, String.format(
                        "Trait `%s` cannot be applied to `%s`. This trait may only be applied "
                        + "to shapes that match the following selector: %s",
                        Trait.getIdiomaticTraitName(test.trait.toShapeId()),
                        shape.getId(), test.selector)));
            }
        }

        return events;
    }

    private Collection<SelectorTest> createTests(Model model) {
        Map<ShapeId, SelectorTest> tests = new HashMap<>();
        Map<ShapeId, Selector> selectors = new HashMap<>();

        for (Shape shape : model.toSet()) {
            for (Trait trait : shape.getAllTraits().values()) {
                // The trait selector has to be resolved against the model,
                // and possibly defaulted. This just caches that result since
                // it's called for every single trait applied to a shape.
                Selector selector = selectors.computeIfAbsent(trait.toShapeId(), id -> resolveSelector(id, model));

                // Only need to test the location for traits that have some
                // kind of constraint.
                if (!selector.toString().trim().equals("*")) {
                    SelectorTest test = tests.computeIfAbsent(
                            trait.toShapeId(),
                            id -> new SelectorTest(trait, selector));
                    test.appliedTo.add(shape);
                }
            }
        }

        return tests.values();
    }

    private Selector resolveSelector(ShapeId id, Model model) {
        return model.getTraitDefinition(id)
                .map(TraitDefinition::getSelector)
                .orElse(Selector.IDENTITY);
    }

    private static final class SelectorTest {
        final Trait trait;
        final Selector selector;
        final Set<Shape> appliedTo = new HashSet<>();

        SelectorTest(Trait trait, Selector selector) {
            this.trait = trait;
            this.selector = selector;
        }
    }
}
