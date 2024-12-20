/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.protocoltests.traits;

import java.util.Locale;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ToNode;

/**
 * Indicates that a compliance test case is only to be implemented by
 * "client" or "server" implementations.
 */
public enum AppliesTo implements ToNode {
    CLIENT,
    SERVER;

    @Override
    public String toString() {
        return super.toString().toLowerCase(Locale.ENGLISH);
    }

    public static AppliesTo fromNode(Node node) {
        String value = node.expectStringNode()
                .expectOneOf("client", "server")
                .toUpperCase(Locale.ENGLISH);
        return AppliesTo.valueOf(value);
    }

    @Override
    public Node toNode() {
        return Node.from(toString());
    }
}
