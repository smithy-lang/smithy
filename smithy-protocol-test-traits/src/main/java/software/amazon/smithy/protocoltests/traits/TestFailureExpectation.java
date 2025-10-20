/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocoltests.traits;

import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Defines the expected failure of a test.
 *
 * <p>This can be any error response, or a specific error response.
 */
public final class TestFailureExpectation implements ToNode {
    private static final String ERROR_ID = "errorId";

    private final ShapeId errorId;

    private TestFailureExpectation(ShapeId errorId) {
        this.errorId = errorId;
    }

    /**
     * @return Returns an expectation that a test will fail with an unspecified error.
     */
    public static TestFailureExpectation anyError() {
        return new TestFailureExpectation(null);
    }

    /**
     * Create an expectation that a test will fail with a specified error.
     *
     * @param errorId Shape ID of the expected error.
     *
     * @return Returns a specific error expectation.
     */
    public static TestFailureExpectation errorWithId(ShapeId errorId) {
        return new TestFailureExpectation(errorId);
    }

    /**
     * Gets the ID of the expected error shape.
     *
     * <p>If present, it indicates the test should throw a matching error.
     * Otherwise, the test should throw any error.
     *
     * @return The ID of the expected error shape.
     */
    public Optional<ShapeId> getErrorId() {
        return Optional.ofNullable(errorId);
    }

    @Override
    public Node toNode() {
        return Node.objectNodeBuilder()
                .withOptionalMember(ERROR_ID, this.getErrorId().map(ShapeId::toString).map(StringNode::from))
                .build();
    }

    public static TestFailureExpectation fromNode(Node node) {
        ObjectNode o = node.expectObjectNode();
        return o.getStringMember(ERROR_ID)
                .map(ShapeId::fromNode)
                .map(TestFailureExpectation::errorWithId)
                .orElse(TestFailureExpectation.anyError());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || o.getClass() != getClass()) {
            return false;
        } else {
            return toNode().equals(((TestFailureExpectation) o).toNode());
        }
    }

    @Override
    public int hashCode() {
        return toNode().hashCode();
    }
}
