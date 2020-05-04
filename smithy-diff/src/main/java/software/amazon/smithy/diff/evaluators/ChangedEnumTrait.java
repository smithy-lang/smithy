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

package software.amazon.smithy.diff.evaluators;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.diff.ChangedShape;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.EnumDefinition;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.Pair;

/**
 * Emits a NOTE when a new enum value is added, emits an ERROR when an
 * enum value is removed, and emits an ERROR when an enum name changes.
 */
public final class ChangedEnumTrait extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return differences.changedShapes()
                .flatMap(change -> OptionalUtils.stream(change.getChangedTrait(EnumTrait.class))
                        .map(p -> Pair.of(change, p)))
                .flatMap(pair -> validateEnum(pair.getLeft(), pair.getRight()).stream())
                .collect(Collectors.toList());
    }

    private List<ValidationEvent> validateEnum(ChangedShape<Shape> change, Pair<EnumTrait, EnumTrait> trait) {
        EnumTrait oldTrait = trait.getLeft();
        EnumTrait newTrait = trait.getRight();
        List<ValidationEvent> events = new ArrayList<>();

        for (EnumDefinition definition : oldTrait.getValues()) {
            Optional<EnumDefinition> maybeNewValue = newTrait.getValues().stream()
                    .filter(d -> d.getValue().equals(definition.getValue()))
                    .findFirst();

            if (!maybeNewValue.isPresent()) {
                events.add(error(change.getNewShape(), String.format(
                        "Enum value `%s` was removed", definition.getValue())));
            } else {
                EnumDefinition newValue = maybeNewValue.get();
                if (!newValue.getName().equals(definition.getName())) {
                    events.add(error(change.getNewShape(), String.format(
                            "Enum `name` changed from `%s` to `%s` for the `%s` value",
                            definition.getName().orElse(null),
                            newValue.getName().orElse(null),
                            definition.getValue())));
                }
            }
        }

        for (EnumDefinition definition : newTrait.getValues()) {
            if (!oldTrait.getEnumDefinitionValues().contains(definition.getValue())) {
                events.add(note(change.getNewShape(), String.format(
                        "Enum value `%s` was added", definition.getValue())));
            }
        }

        return events;
    }
}
