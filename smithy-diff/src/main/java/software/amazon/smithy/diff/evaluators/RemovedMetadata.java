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
 * Creates a DANGER event when metadata is removed.
 */
public final class RemovedMetadata extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return differences.removedMetadata()
                .map(metadata -> ValidationEvent.builder()
                        .id(getEventId() + "." + metadata.getLeft())
                        .severity(Severity.DANGER)
                        .sourceLocation(metadata.getRight().getSourceLocation())
                        .message(String.format(
                                "Metadata key `%s` was removed with the following value: %s",
                                metadata.getLeft(),
                                Node.prettyPrintJson(metadata.getRight())))
                        .build())
                .collect(Collectors.toList());
    }
}
