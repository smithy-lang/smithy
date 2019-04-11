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
import java.util.stream.Collectors;
import software.amazon.smithy.diff.ChangedShape;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.Pair;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.EnumConstantBody;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.OptionalUtils;

/**
 * Emits a NOTE when a new enum value is added, emits an ERROR when an
 * enum value is removed, and emits an ERROR when an enum name changes.
 */
public class ChangedEnumTrait extends AbstractDiffEvaluator {
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

        oldTrait.getValues().forEach((key, value) -> {
            if (!newTrait.getValues().containsKey(key)) {
                events.add(error(change.getNewShape(), String.format("Enum value `%s` was removed", key)));
            } else {
                EnumConstantBody newValue = newTrait.getValues().get(key);
                if (!newValue.getName().equals(value.getName())) {
                    events.add(error(change.getNewShape(), String.format(
                            "Enum `name` changed from `%s` to `%s` for the `%s` value",
                            value.getName().orElse(null),
                            newValue.getName().orElse(null),
                            key)));
                }
            }
        });

        newTrait.getValues().forEach((key, value) -> {
            if (!oldTrait.getValues().containsKey(key)) {
                events.add(note(change.getNewShape(), String.format("Enum value `%s` was added", key)));
            }
        });

        return events;
    }
}
