/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.diff.evaluators;

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.diff.Differences;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Creates a WARNING event when metadata is changed.
 */
public final class ChangedMetadata extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return differences.changedMetadata()
                .map(metadata -> ValidationEvent.builder()
                        .id(getEventId())
                        .severity(Severity.WARNING)
                        .sourceLocation(metadata)
                        .message(String.format(
                                "Metadata key `%s` was changed from %s to %s",
                                metadata.getKey(),
                                Node.prettyPrintJson(metadata.getOldValue()),
                                Node.prettyPrintJson(metadata.getNewValue())))
                        .build())
                .collect(Collectors.toList());
    }
}
