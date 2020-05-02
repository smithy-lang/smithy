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
import java.util.List;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.LengthTrait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.Pair;

public final class LengthTraitValidator extends AbstractValidator {
    @Override
    public List<ValidationEvent> validate(Model model) {
        List<ValidationEvent> events = new ArrayList<>();
        for (Shape shape : model.getShapesWithTrait(LengthTrait.class)) {
            events.addAll(validateLengthTrait(shape, shape.expectTrait(LengthTrait.class)));
        }

        return events;
    }

    private List<ValidationEvent> validateLengthTrait(Shape shape, LengthTrait trait) {
        List<ValidationEvent> events = new ArrayList<>();
        trait.getMin()
                .filter(min -> min < 0)
                .map(min -> error(shape, trait, "A length trait is applied with a negative `min` value."))
                .ifPresent(events::add);

        trait.getMax()
                .filter(max -> max < 0)
                .map(max -> error(shape, trait, "A length trait is applied with a negative `max` value."))
                .ifPresent(events::add);

        trait.getMin()
                .flatMap(min -> trait.getMax().map(max -> Pair.of(min, max)))
                .filter(pair -> pair.getLeft() > pair.getRight())
                .map(pair -> error(shape, trait, "A length trait is applied with a `min` value greater than "
                        + "its `max` value."))
                .map(events::add);
        return events;
    }
}
