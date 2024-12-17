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
 * Creates an ERROR event when a trait definition is removed.
 */
public final class RemovedTraitDefinition extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return differences.removedShapes()
                .filter(shape -> shape.hasTrait(TraitDefinition.class))
                .map(shape -> ValidationEvent.builder()
                        .id(getEventId())
                        .severity(Severity.ERROR)
                        .shape(shape)
                        .sourceLocation(shape.expectTrait(TraitDefinition.class).getSourceLocation())
                        .message(String.format("Trait definition `%s` was removed", shape.getId()))
                        .build())
                .collect(Collectors.toList());
    }
}
