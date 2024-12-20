/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import software.amazon.smithy.diff.ChangedShape;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.Pair;

/**
 * Implements the shared logic for length and range trait validation.
 *
 * @param <T> LengthTrait or RangeTrait types to validate.
 */
abstract class AbstractLengthAndRangeValidator<T extends Trait> extends AbstractDiffEvaluator {
    @Override
    public final List<ValidationEvent> evaluate(Differences differences) {
        return differences.changedShapes()
                .flatMap(change -> {
                    Pair<T, T> pair = change.getChangedTrait(getTraitType()).orElse(null);
                    return pair == null
                            ? Stream.empty()
                            : validateTrait(change, pair).stream();
                })
                .collect(Collectors.toList());
    }

    abstract Class<T> getTraitType();

    abstract Optional<BigDecimal> getMin(T trait);

    abstract Optional<BigDecimal> getMax(T trait);

    private List<ValidationEvent> validateTrait(ChangedShape<Shape> change, Pair<T, T> changedTrait) {
        List<ValidationEvent> events = new ArrayList<>();
        T oldTrait = changedTrait.getLeft();
        T newTrait = changedTrait.getRight();
        BigDecimal oldMin = getMin(oldTrait).orElse(BigDecimal.ZERO);
        BigDecimal newMin = getMin(newTrait).orElse(BigDecimal.ZERO);
        BigDecimal oldMax = getMax(oldTrait).orElse(null);
        BigDecimal newMax = getMax(newTrait).orElse(null);

        if (newMin.compareTo(oldMin) > 0) {
            events.add(error(change.getNewShape(),
                    String.format(
                            "%s trait value `min` was made more restrictive by raising from %s to %s",
                            newTrait.toShapeId(),
                            oldMin,
                            newMin)));
        }

        if (newMax != null && (oldMax == null || oldMax.compareTo(newMax) > 0)) {
            events.add(error(change.getNewShape(),
                    String.format(
                            "%s trait value `max` was made more restrictive by lowering from %s to %s",
                            newTrait.toShapeId(),
                            oldMax,
                            newMax)));
        }

        return events;
    }
}
