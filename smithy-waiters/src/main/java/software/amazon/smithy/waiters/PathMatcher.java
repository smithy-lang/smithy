/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.waiters;

import java.util.Objects;
import java.util.Set;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.SetUtils;

/**
 * A {@link Matcher} implementation for {@code inputPathList},
 * {@code outputPathList}, and {@code errorPathList}.
 */
public final class PathMatcher implements ToNode {

    private static final String EXPECTED = "expected";
    private static final String PATH = "path";
    private static final String COMPARATOR = "comparator";
    private static final Set<String> KEYS = SetUtils.of(EXPECTED, PATH, COMPARATOR);

    private final String path;
    private final String expected;
    private final PathComparator comparator;

    /**
     * @param path The path to execute.
     * @param expected The expected value of the path.
     * @param comparator Comparison performed on the list value.
     */
    public PathMatcher(String path, String expected, PathComparator comparator) {
        this.path = path;
        this.expected = expected;
        this.comparator = comparator;
    }

    /**
     * Gets the path to execute.
     *
     * @return Returns the path to execute.
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets the expected return value of each element returned by the
     * path.
     *
     * @return The return value to compare each result against.
     */
    public String getExpected() {
        return expected;
    }

    /**
     * Gets the comparison performed on the list.
     *
     * @return Returns the comparator.
     */
    public PathComparator getComparator() {
        return comparator;
    }

    /**
     * Creates a new instance from a {@link Node}.
     *
     * @param node Node tom create the PathMatcher from.
     * @return Returns the created PathMatcher.
     * @throws ExpectationNotMetException if the given Node is invalid.
     */
    public static PathMatcher fromNode(Node node) {
        ObjectNode value = node.expectObjectNode().warnIfAdditionalProperties(KEYS);
        return new PathMatcher(value.expectStringMember(PATH).getValue(),
                value.expectStringMember(EXPECTED).getValue(),
                PathComparator.fromNode(value.expectStringMember(COMPARATOR)));
    }

    @Override
    public Node toNode() {
        return Node.objectNode()
                .withMember(PATH, Node.from(path))
                .withMember(EXPECTED, Node.from(expected))
                .withMember(COMPARATOR, comparator.toNode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof PathMatcher)) {
            return false;
        }

        PathMatcher that = (PathMatcher) o;
        return getPath().equals(that.getPath())
                && getComparator().equals(that.getComparator())
                && getExpected().equals(that.getExpected());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getPath(), getComparator(), getExpected());
    }
}
