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
 * Represents an acceptor in a waiter's state machine.
 */
public final class Acceptor implements ToNode {

    private static final String STATE = "state";
    private static final String MATCHER = "matcher";
    private static final Set<String> KEYS = SetUtils.of(STATE, MATCHER);

    private final AcceptorState state;
    private final Matcher<?> matcher;

    /**
     * @param state State the acceptor transitions to when matched.
     * @param matcher The matcher to match against.
     */
    public Acceptor(AcceptorState state, Matcher<?> matcher) {
        this.state = state;
        this.matcher = matcher;
    }

    /**
     * Gets the state to transition to if matched.
     *
     * @return Acceptor state to transition to.
     */
    public AcceptorState getState() {
        return state;
    }

    /**
     * Gets the matcher used to test if the acceptor.
     *
     * @return Returns the matcher.
     */
    public Matcher<?> getMatcher() {
        return matcher;
    }

    /**
     * Creates an Acceptor from a {@link Node}.
     *
     * @param node Node to create the Acceptor from.
     * @return Returns the created Acceptor.
     * @throws ExpectationNotMetException if the given Node is invalid.
     */
    public static Acceptor fromNode(Node node) {
        ObjectNode value = node.expectObjectNode().warnIfAdditionalProperties(KEYS);
        return new Acceptor(AcceptorState.fromNode(value.expectStringMember(STATE)),
                Matcher.fromNode(value.expectMember(MATCHER)));
    }

    @Override
    public Node toNode() {
        return Node.objectNode()
                .withMember("state", Node.from(state.toString()))
                .withMember("matcher", matcher.toNode());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof Acceptor)) {
            return false;
        }
        Acceptor acceptor = (Acceptor) o;
        return getState() == acceptor.getState() && Objects.equals(getMatcher(), acceptor.getMatcher());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getState(), getMatcher());
    }
}
