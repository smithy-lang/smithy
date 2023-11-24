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
