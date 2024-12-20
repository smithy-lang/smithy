/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.validation;

import java.util.Arrays;
import java.util.Optional;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ToNode;

/**
 * Severity level of a validation exception.
 */
public enum Severity implements ToNode {
    SUPPRESSED,
    NOTE,
    WARNING,
    DANGER,
    ERROR;

    private static final String[] NAMES = {"SUPPRESSED", "NOTE", "WARNING", "DANGER", "ERROR"};

    /**
     * Check if the severity level of the error can be suppressed.
     *
     * @return Returns true if a suppression can silence errors at this
     *  severity level.
     */
    public boolean canSuppress() {
        return this != ERROR && this != SUPPRESSED;
    }

    /**
     * Create a new Severity from a string.
     *
     * @param text Text to convert into a severity level.
     * @return Returns the format in an Optional.
     */
    public static Optional<Severity> fromString(String text) {
        return Arrays.stream(values()).filter(value -> value.toString().equals(text)).findFirst();
    }

    /**
     * Creates a severity value from a node.
     *
     * @param node Node to parse.
     * @return Returns the parsed Severity.
     * @throws ExpectationNotMetException if the node is invalid.
     */
    public static Severity fromNode(Node node) {
        String value = node.expectStringNode().expectOneOf(NAMES);
        return Severity.valueOf(value);
    }

    @Override
    public Node toNode() {
        return Node.from(toString());
    }
}
