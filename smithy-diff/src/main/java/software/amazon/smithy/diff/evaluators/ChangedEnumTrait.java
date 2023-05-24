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

import static software.amazon.smithy.diff.evaluators.ChangedShapeType.expectedStringToEnumChange;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import software.amazon.smithy.diff.ChangedShape;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.EnumDefinition;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.synthetic.SyntheticEnumTrait;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;
import software.amazon.smithy.utils.OptionalUtils;
import software.amazon.smithy.utils.Pair;

/**
 * Emits a NOTE when a new enum value is appended, emits an ERROR when an
 * enum value is removed, emits an ERROR when an enum name changes, and
 * emits an ERROR when a new enum value is inserted before the end of the
 * list of existing values.
 */
public final class ChangedEnumTrait extends AbstractDiffEvaluator {
    private static final String ORDER_CHANGED = ".OrderChanged";
    private static final String NAME_CHANGED = ".NameChanged";
    private static final String REMOVED = ".Removed";

    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return differences.changedShapes()
                .flatMap(change -> OptionalUtils.stream(getChangedEnumTraitPair(change))
                        .map(p -> Pair.of(change, p)))
                .flatMap(pair -> validateEnum(pair.getLeft(), pair.getRight()).stream())
                .collect(Collectors.toList());
    }

    private Optional<Pair<EnumTrait, EnumTrait>> getChangedEnumTraitPair(ChangedShape<Shape> change) {
        // Change between two enum traits
        Optional<Pair<EnumTrait, EnumTrait>> changedEnumTrait = change.getChangedTrait(EnumTrait.class);
        if (changedEnumTrait.isPresent()) {
            return changedEnumTrait;
        }
        // Change between enum trait in old model and enum shape synthetic enum trait in new model
        if (expectedStringToEnumChange(change)) {
            return Optional.of(Pair.of(
                    change.getOldShape().expectTrait(EnumTrait.class),
                    change.getNewShape().expectTrait(SyntheticEnumTrait.class)));
        }
        return Optional.empty();
    }

    private List<ValidationEvent> validateEnum(ChangedShape<Shape> change, Pair<EnumTrait, EnumTrait> trait) {
        EnumTrait oldTrait = trait.getLeft();
        EnumTrait newTrait = trait.getRight();
        List<ValidationEvent> events = new ArrayList<>();
        int oldEndPosition = oldTrait.getValues().size() - 1;

        for (EnumDefinition definition : oldTrait.getValues()) {
            Optional<EnumDefinition> maybeNewValue = newTrait.getValues().stream()
                    .filter(d -> d.getValue().equals(definition.getValue()))
                    .findFirst();

            if (!maybeNewValue.isPresent()) {
                events.add(
                        ValidationEvent.builder()
                                .severity(Severity.ERROR)
                                .message(String.format("Enum value `%s` was removed", definition.getValue()))
                                .shape(change.getNewShape())
                                .id(getEventId() + REMOVED)
                                .build()
                );
                oldEndPosition--;
            } else {
                EnumDefinition newValue = maybeNewValue.get();
                if (!newValue.getName().equals(definition.getName())) {
                    events.add(
                            ValidationEvent.builder()
                                    .severity(Severity.ERROR)
                                    .message(String.format(
                                            "Enum `name` changed from `%s` to `%s` for the `%s` value",
                                            definition.getName().orElse(null),
                                            newValue.getName().orElse(null),
                                            definition.getValue()))
                                    .shape(change.getNewShape())
                                    .id(getEventId() + NAME_CHANGED)
                                    .build()
                    );
                }
            }
        }

        int newPosition = 0;
        for (EnumDefinition definition : newTrait.getValues()) {
            if (!oldTrait.getEnumDefinitionValues().contains(definition.getValue())) {
                if (newPosition <= oldEndPosition) {
                    events.add(
                            ValidationEvent.builder()
                                    .severity(Severity.ERROR)
                                    .message(String.format(
                                    "Enum value `%s` was inserted before the end of the list of existing values. This "
                                            + "can cause compatibility issues when ordinal values are used for "
                                            + "iteration, serialization, etc.", definition.getValue()))
                                    .shape(change.getNewShape())
                                    .id(getEventId() + ORDER_CHANGED)
                                    .build()
                    );
                } else {
                    events.add(note(change.getNewShape(), String.format(
                            "Enum value `%s` was appended", definition.getValue())));
                }
            }

            newPosition++;
        }

        return events;
    }
}
