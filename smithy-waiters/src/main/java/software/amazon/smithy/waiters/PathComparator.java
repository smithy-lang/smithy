/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.waiters;

import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ToNode;

/**
 * Defines a comparison to perform in a PathMatcher.
 */
public enum PathComparator implements ToNode {

    /** Matches if all values in the list matches the expected string. */
    ALL_STRING_EQUALS("allStringEquals"),

    /** Matches if any value in the list matches the expected string. */
    ANY_STRING_EQUALS("anyStringEquals"),

    /** Matches if the return value is a string that is equal to the expected string. */
    STRING_EQUALS("stringEquals"),

    /** Matches if the return value is a boolean that is equal to the string literal 'true' or 'false'. */
    BOOLEAN_EQUALS("booleanEquals");

    private final String asString;

    PathComparator(String asString) {
        this.asString = asString;
    }

    /**
     * Creates a {@code PathComparator} from a {@link Node}.
     * @param node Node to create the {@code PathComparator} from.
     * @return Returns the created {@code PathComparator}.
     * @throws ExpectationNotMetException if the given {@code node} is invalid.
     */
    public static PathComparator fromNode(Node node) {
        String value = node.expectStringNode().getValue();
        for (PathComparator comparator : values()) {
            if (comparator.toString().equals(value)) {
                return comparator;
            }
        }

        throw new ExpectationNotMetException("Expected valid path comparator, but found " + value, node);
    }

    @Override
    public String toString() {
        return asString;
    }

    @Override
    public Node toNode() {
        return Node.from(toString());
    }
}
