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
                                metadata.getLeft(), Node.prettyPrintJson(metadata.getRight())))
                        .build())
                .collect(Collectors.toList());
    }
}
