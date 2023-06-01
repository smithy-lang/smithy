/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.parameters;

import java.util.Objects;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;

public final class Deprecated implements ToNode {
    private static final String MESSAGE = "message";
    private static final String SINCE = "since";

    private final String message;
    private final String since;

    public Deprecated(String message, String since) {
        this.message = message;
        this.since = since;
    }

    public static Deprecated fromNode(ObjectNode objectNode) {
        String message = objectNode.getStringMember(MESSAGE)
                                 .map(StringNode::getValue)
                                 .orElse(null);
        String since = objectNode.getStringMember(SINCE)
                               .map(StringNode::getValue)
                               .orElse(null);
        return new Deprecated(message, since);
    }

    public String getMessage() {
        return message;
    }

    public String getSince() {
        return since;
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
