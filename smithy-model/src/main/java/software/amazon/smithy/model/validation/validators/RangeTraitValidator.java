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

import static java.lang.String.format;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.RangeTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.AbstractValidator;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.Pair;

/**
 * Ensures that range traits are valid.
 */
public class RangeTraitValidator extends AbstractValidator {

    @Override
    public List<ValidationEvent> validate(Model model) {
        return model.shapes()
                .flatMap(shape -> Trait.flatMapStream(shape, RangeTrait.class))
                .flatMap(pair -> validateRangeTrait(model, pair.getLeft(), pair.getRight()).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateRangeTrait(Model model, Shape shape, RangeTrait trait) {
        List<ValidationEvent> events = new ArrayList<>();
        trait.getMin()
                .flatMap(min -> validateRangeProperty(model, shape, trait, min, "min"))
                .ifPresent(events::add);

        trait.getMax()
                .flatMap(max -> validateRangeProperty(model, shape, trait, max, "max"))
                .ifPresent(events::add);

        // Makes sure that `min` is less than `max`
        trait.getMin()
                .flatMap(min -> trait.getMax().map(max -> Pair.of(min, max)))
                .filter(pair -> pair.getLeft().compareTo(pair.getRight()) > 0)
                .map(pair -> error(shape, trait, "A range trait is applied with a `min` value greater than "
                        + "its `max` value."))
                .map(events::add);

        return events;
    }

    private Optional<ValidationEvent> validateRangeProperty(
            Model model,
            Shape shape,
            RangeTrait trait,
            BigDecimal property,
            String name
    ) {
        if (!property.remainder(BigDecimal.ONE).equals(BigDecimal.ZERO)) {
            if (shape.isMemberShape()) {
                MemberShape member = shape.expectMemberShape();
                Optional<Shape> target = model.getShape(member.getTarget());
                if (target.isPresent() && !isDecimalShape(target.get())) {
                    return Optional.of(error(shape, trait, format(
                            "Member `%s` is marked with the `range` trait, but its `%s` property "
                            + "is a decimal (%s) when its target (`%s`) does not support decimals.",
                            shape.getId(), name, property, target.get().getId())));
                }
            } else if (!isDecimalShape(shape)) {
                return Optional.of(error(shape, trait, format(
                        "Shape `%s` is marked with the `range` trait, but its `%s` property "
                        + "is a decimal (%s) when its shape (`%s`) does not support decimals.",
                        shape.getId(), name, property, shape.getType())));
            }
        }
        return Optional.empty();
    }

    private boolean isDecimalShape(Shape shape) {
        return shape.isFloatShape() || shape.isDoubleShape() || shape.isBigDecimalShape();
    }
}
