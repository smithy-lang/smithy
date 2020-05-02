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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
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
        Collection<SelectorTest> tests = createTests(model);

        for (SelectorTest test : tests) {
            // Find the shapes that this trait can be applied to.
            Set<Shape> matches = test.selector.select(model);

            // Remove the allowed locations from the real locations, leaving only
            // the shapes in the set that are invalid.
            test.appliedTo.removeAll(matches);

            for (Shape shape : test.appliedTo) {
                // Strip out newlines with successive spaces.
                String sanitized = SANITIZE.matcher(test.selector.toString()).replaceAll(" ");
                events.add(error(shape, shape.findTrait(test.trait).get(), String.format(
                        "Trait `%s` cannot be applied to `%s`. This trait may only be applied "
                        + "to shapes that match the following selector: %s",
                        Trait.getIdiomaticTraitName(test.trait.toShapeId()),
                        shape.getId(),
                        sanitized)));
            }
        }

        return events;
    }

    private Collection<SelectorTest> createTests(Model model) {
        List<SelectorTest> tests = new ArrayList<>(model.getAppliedTraits().size());

        for (ShapeId traitId : model.getAppliedTraits()) {
            // This set is mutated later, so make a copy.
            Set<Shape> shapes = new HashSet<>(model.getShapesWithTrait(traitId));
            model.getTraitDefinition(traitId).ifPresent(definition -> {
                tests.add(new SelectorTest(traitId, definition.getSelector(), shapes));
            });
        }

        return tests;
    }

    private static final class SelectorTest {
        final ShapeId trait;
        final Selector selector;
        final Set<Shape> appliedTo;

        SelectorTest(ShapeId trait, Selector selector, Set<Shape> appliedTo) {
            this.trait = trait;
            this.selector = selector;
            this.appliedTo = appliedTo;
        }
    }
}
