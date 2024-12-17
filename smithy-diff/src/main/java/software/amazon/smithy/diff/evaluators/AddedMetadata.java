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
 * Creates a NOTE event when metadata is added.
 */
public final class AddedMetadata extends AbstractDiffEvaluator {
    @Override
    public List<ValidationEvent> evaluate(Differences differences) {
        return differences.addedMetadata()
                .map(metadata -> ValidationEvent.builder()
                        .id(getEventId())
                        .severity(Severity.NOTE)
                        .sourceLocation(metadata.getRight().getSourceLocation())
                        .message(String.format(
                                "Metadata key `%s` was added with the following value: %s",
                                metadata.getLeft(),
                                Node.prettyPrintJson(metadata.getRight())))
                        .build())
                .collect(Collectors.toList());
    }
}
