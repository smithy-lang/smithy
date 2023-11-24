/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.parameters;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;

/**
 * Container for information on a deprecated parameter.
 */
public final class Deprecated implements ToNode {
    private static final String MESSAGE = "message";
    private static final String SINCE = "since";

    private final String message;
    private final String since;

    private Deprecated(String message, String since) {
        this.message = message;
        this.since = since;
    }

    /**
     * Creates a {@link Deprecated} of a specific type from the given Node information.
     *
     * @param objectNode the node to deserialize.
     * @return the created Deprecated.
     */
    public static Deprecated fromNode(ObjectNode objectNode) {
        String message = objectNode.getStringMemberOrDefault(MESSAGE, null);
        String since = objectNode.getStringMemberOrDefault(SINCE, null);
        return new Deprecated(message, since);
    }

    /**
     * Gets the deprecation message value.
     *
     * @return returns the optional deprecation message value.
     */
    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }

    /**
     * Gets the deprecated since value.
     *
     * @return returns the optional deprecated since value.
     */
    public Optional<String> getSince() {
        return Optional.ofNullable(since);
    }

    @Override
    public Node toNode() {
        NodeMapper mapper = new NodeMapper();
        mapper.disableToNodeForClass(Deprecated.class);
        return mapper.serialize(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        Deprecated that = (Deprecated) obj;
        return Objects.equals(this.message, that.message)
                       && Objects.equals(this.since, that.since);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, since);
    }

    @Override
    public String toString() {
        return "Deprecated["
                       + "message=" + message + ", "
                       + "since=" + since + ']';
    }
}
