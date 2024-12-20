/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Creates a NOTE event when a trait definition is added.
 */
public final class AddedTraitDefinition extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return differences.addedShapes()
                .filter(shape -> shape.hasTrait(TraitDefinition.class))
                .map(shape -> ValidationEvent.builder()
                        .id(getEventId())
                        .severity(Severity.NOTE)
                        .shape(shape)
                        .message(String.format("Trait definition `%s` was added", shape.getId()))
                        .build())
                .collect(Collectors.toList());
    }
}
