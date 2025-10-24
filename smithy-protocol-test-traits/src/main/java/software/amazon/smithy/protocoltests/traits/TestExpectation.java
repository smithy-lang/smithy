/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocoltests.traits;

import java.util.Optional;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.validation.ValidationUtils;
import software.amazon.smithy.utils.ListUtils;

/**
 * Defines the expected result of a test case.
 *
 * <p>This can either be a successful response, any error response, or a
 * specific error response.
 */
public final class TestExpectation implements ToNode {
    private static final String SUCCESS = "success";
    private static final String FAILURE = "failure";

    private final TestFailureExpectation failure;

    private TestExpectation(TestFailureExpectation failure) {
        this.failure = failure;
    }

    /**
     * @return Creates an expectation that the service call for a smoke
     * test case is successful.
     */
    public static TestExpectation success() {
        return new TestExpectation(null);
    }

    /**
     * @param failure The failure to expect.
     * @return Creates an expectation that the service call for a smoke test
     * case will result in the given failure.
     */
    public static TestExpectation failure(TestFailureExpectation failure) {
        return new TestExpectation(failure);
    }

    /**
     * Creates a {@link TestExpectation} from a {@link Node}.
     *
     * @param node Node to deserialize into a {@link TestExpectation}.
     * @return Returns the created {@link TestExpectation}.
     */
    public static TestExpectation fromNode(Node node) {
        ObjectNode o = node.expectObjectNode();
        if (o.containsMember(SUCCESS)) {
            o.expectNoAdditionalProperties(ListUtils.of(SUCCESS));
            return TestExpectation.success();
        } else if (o.containsMember(FAILURE)) {
            o.expectNoAdditionalProperties(ListUtils.of(FAILURE));
            TestFailureExpectation failure = TestFailureExpectation.fromNode(o.expectObjectMember(FAILURE));
            return TestExpectation.failure(failure);
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
    public Optional<TestFailureExpectation> getFailure() {
        return Optional.ofNullable(failure);
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder builder = Node.objectNodeBuilder();
        if (this.isSuccess()) {
            builder.withMember(SUCCESS, Node.objectNode());
        } else {
            Node failureNode = this.getFailure()
                    .map(TestFailureExpectation::toNode)
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
            return toNode().equals(((TestExpectation) o).toNode());
        }
    }

    @Override
    public int hashCode() {
        return toNode().hashCode();
    }
}
