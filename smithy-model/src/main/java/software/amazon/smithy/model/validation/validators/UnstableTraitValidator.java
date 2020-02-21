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

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.UnstableTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Emits a validation event if a model contains shapes that are bound to unstable traits.
 */
public final class UnstableTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        Set<ShapeId> unstableTraits = model.getTraitShapes().stream()
                .filter(trait -> trait.hasTrait(UnstableTrait.class))
                .map(Shape::getId)
                .collect(Collectors.toSet());

        return model.shapes()
                .flatMap(shape -> validateShape(shape, unstableTraits).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateShape(Shape shape, Set<ShapeId> unstableTraits) {
        List<ValidationEvent> events = new ArrayList<>();
        shape.getAllTraits().forEach((shapeId, trait) -> {
            if (!unstableTraits.contains(trait.toShapeId())) {
                return;
            }
            events.add(warning(shape, trait, format("This shape applies a trait that is unstable: %s",
                    trait.toShapeId())));
        });
        return events;
    }
}
