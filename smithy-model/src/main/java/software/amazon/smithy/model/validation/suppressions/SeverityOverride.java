/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation.suppressions;

import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.validation.Severity;
import software.amazon.smithy.model.validation.ValidationEvent;

/**
 * Changes the severity of a validation event.
 */
@FunctionalInterface
public interface SeverityOverride {

    /**
     * Returns the severity to apply to the validation event.
     *
     * @param event Event to modify.
     * @return Returns the severity to use with the event.
     */
    Severity apply(ValidationEvent event);

    /**
     * Creates a severity override from a {@link Node} found in the "severityOverrides" metadata of a Smithy model.
     *
     * @param node Node to parse.
     * @return Returns the loaded override.
     * @throws ExpectationNotMetException if the override node is malformed.
     */
    static SeverityOverride fromMetadata(Node node) {
        return MetadataSeverityOverride.fromNode(node);
    }
}
