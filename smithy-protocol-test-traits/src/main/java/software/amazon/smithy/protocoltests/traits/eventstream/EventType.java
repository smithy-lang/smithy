/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocoltests.traits.eventstream;

import java.util.Locale;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ToNode;

/**
 * The different types of event that are able to be sent over event streams.
 */
public enum EventType implements ToNode {
    /**
     * Indicates the event is a request message.
     */
    REQUEST,

    /**
     * Indicates the event is a response message.
     */
    RESPONSE;

    @Override
    public String toString() {
        return super.toString().toLowerCase(Locale.ENGLISH);
    }

    public static EventType fromNode(Node node) {
        return EventType.valueOf(
                node.expectStringNode().expectOneOf("request", "response").toUpperCase(Locale.ENGLISH));
    }

    @Override
    public Node toNode() {
        return Node.from(toString());
    }
}
