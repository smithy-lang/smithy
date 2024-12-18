/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.smoketests.traits;

import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.ShapeId;

/**
 * Defines the expected failure of a service call for a smoke test case.
 *
 * <p>This can be any error response, or a specific error response.
 */
public final class FailureExpectation implements ToNode {
    private static final String ERROR_ID = "errorId";

    private final ShapeId errorId;

    private FailureExpectation(ShapeId errorId) {
        this.errorId = errorId;
    }

    /**
     * @return Creates a failure expectation that the service call will result
     * in any error response.
     */
    public static FailureExpectation anyError() {
        return new FailureExpectation(null);
    }

    /**
     * @param errorId Shape ID of the expected error.
     * @return Creates a failure expectation that the service call will result
     * in an error matching the given shape ID.
     */
    public static FailureExpectation errorWithId(ShapeId errorId) {
        return new FailureExpectation(errorId);
    }

    /**
     * Gets the ID of the expected error shape.
     *
     * <p>If present, it indicates the call should throw a matching error.
     * Otherwise, the call should throw any error.
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

    public static FailureExpectation fromNode(Node node) {
        ObjectNode o = node.expectObjectNode();
        return o.getStringMember(ERROR_ID)
                .map(ShapeId::fromNode)
                .map(FailureExpectation::errorWithId)
                .orElse(FailureExpectation.anyError());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || o.getClass() != getClass()) {
            return false;
        } else {
            return toNode().equals(((FailureExpectation) o).toNode());
        }
    }

    @Override
    public int hashCode() {
        return toNode().hashCode();
    }
}
