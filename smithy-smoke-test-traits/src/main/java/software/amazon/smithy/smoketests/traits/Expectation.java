/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.smoketests.traits;

import java.util.Optional;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.ListUtils;

/**
 * Defines the expected result of the service call for a smoke test case.
 *
 * <p>This can either be a successful response, any error response, or a
 * specific error response.
 */
public final class Expectation implements ToNode {
    private static final String SUCCESS = "success";
    private static final String FAILURE = "failure";

    private final FailureExpectation failure;

    private Expectation(FailureExpectation failure) {
        this.failure = failure;
    }

    /**
     * @return Creates an expectation that the service call for a smoke
     * test case is successful.
     */
    public static Expectation success() {
        return new Expectation(null);
    }

    /**
     * @param failure The failure to expect.
     * @return Creates an expectation that the service call for a smoke test
     * case will result in the given failure.
     */
    public static Expectation failure(FailureExpectation failure) {
        return new Expectation(failure);
    }

    /**
     * Creates a {@link Expectation} from a {@link Node}.
     *
     * @param node Node to deserialize into a {@link Expectation}.
     * @return Returns the created {@link Expectation}.
     */
    public static Expectation fromNode(Node node) {
        ObjectNode o = node.expectObjectNode();
        if (o.containsMember(SUCCESS)) {
            o.expectNoAdditionalProperties(ListUtils.of(SUCCESS));
            return Expectation.success();
        } else if (o.containsMember(FAILURE)) {
            o.expectNoAdditionalProperties(ListUtils.of(FAILURE));
            FailureExpectation failure = FailureExpectation.fromNode(o.expectObjectMember(FAILURE));
            return Expectation.failure(failure);
        } else {
            throw new ExpectationNotMetException("Expected an object with exactly one `" + SUCCESS + "` or `" + FAILURE
                    + "` property, but found properties: " + ValidationUtils.tickedList(o.getStringMap().keySet()), o);
        }
    }

    /**
     * @return Whether the service call is expected to succeed.
     */
    public boolean isSuccess() {
        return failure == null;
    }

    /**
     * @return Whether the service call is expected to fail.
     */
    public boolean isFailure() {
        return failure != null;
    }

    /**
     * @return The expected failure, if this expectation is a failure expectation.
     */
    public Optional<FailureExpectation> getFailure() {
        return Optional.ofNullable(failure);
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder();
        if (this.isSuccess()) {
            builder.withMember(SUCCESS, Node.objectNode());
        } else {
            Node failureNode = this.getFailure()
                    .map(FailureExpectation::toNode)
                    .orElse(Node.objectNode());
            builder.withMember(FAILURE, failureNode);
        }
        return builder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || o.getClass() != getClass()) {
            return false;
        } else {
            return toNode().equals(((Expectation) o).toNode());
        }
    }

    @Override
    public int hashCode() {
        return toNode().hashCode();
    }
}
