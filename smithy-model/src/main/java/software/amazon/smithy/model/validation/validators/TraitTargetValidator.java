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

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.NeighborProviderIndex;
import software.amazon.smithy.model.neighbor.NeighborProvider;
import software.amazon.smithy.model.selector.Selector;
import software.amazon.smithy.model.shapes.Shape;
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
        NeighborProvider neighborProvider = model.getKnowledge(NeighborProviderIndex.class).getProvider();
        return model.shapes()
                .flatMap(shape -> getSelectors(shape, model))
                .filter(check -> !matchesSelector(check, model, neighborProvider))
                .map(check -> error(check.shape, String.format(
                        "Trait `%s` cannot be applied to `%s`. This trait may only be applied to shapes "
                        + "that match the following selector: %s",
                        Trait.getIdiomaticTraitName(check.trait.toShapeId()),
                        check.shape.getId(), check.selector)))
                .collect(Collectors.toList());
    }

    private static final class SelectorCheck {
        final Shape shape;
        final Trait trait;
        final Selector selector;

        SelectorCheck(Shape shape, Trait trait, Selector selector) {
            this.shape = shape;
            this.trait = trait;
            this.selector = selector;
        }
    }

    private Stream<SelectorCheck> getSelectors(Shape shape, Model model) {
        return shape.getAllTraits().values().stream()
                .map(trait -> new SelectorCheck(shape, trait, resolveSelector(trait, model)));
    }

    private Selector resolveSelector(Trait trait, Model model) {
        return model.getTraitDefinition(trait).map(TraitDefinition::getSelector).orElse(Selector.IDENTITY);
    }

    private boolean matchesSelector(
            SelectorCheck check,
            Model model,
            NeighborProvider neighborProvider
    ) {
        return check.selector.select(model, neighborProvider).contains(check.shape);
    }
}
