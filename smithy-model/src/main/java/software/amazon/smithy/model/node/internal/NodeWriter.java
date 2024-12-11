/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node.internal;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.BooleanNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeVisitor;
import software.amazon.smithy.model.node.NullNode;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;

/**
 * Serializes {@link Node} objects to JSON.
 */
final class NodeWriter implements NodeVisitor<Void> {

    private JsonWriter writer;

    NodeWriter(JsonWriter writer) {
        this.writer = writer;
    }

    @Override
    public Void arrayNode(ArrayNode node) {
        try {
            // Simple optimization for empty arrays.
            if (node.isEmpty()) {
                writer.writeLiteral("[]");
                return null;
            }

            writer.writeArrayOpen();
            Iterator<Node> iterator = node.getElements().iterator();
            while (iterator.hasNext()) {
                iterator.next().accept(this);
                if (iterator.hasNext()) {
                    writer.writeArraySeparator();
                }
            }
            writer.writeArrayClose();
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Void booleanNode(BooleanNode node) {
        try {
            writer.writeLiteral(node.getValue() ? "true" : "false");
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Void nullNode(NullNode node) {
        try {
            writer.writeLiteral("null");
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Void numberNode(NumberNode node) {
        try {
            writer.writeNumber(node.toString());
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Void objectNode(ObjectNode node) {
        try {
            // Simple optimization for empty objects.
            if (node.isEmpty()) {
                writer.writeLiteral("{}");
                return null;
            }

            writer.writeObjectOpen();
            Iterator<Map.Entry<StringNode, Node>> iterator = node.getMembers().entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<StringNode, Node> entry = iterator.next();
                writer.writeMemberName(entry.getKey().getValue());
                writer.writeMemberSeparator();
                entry.getValue().accept(this);
                if (iterator.hasNext()) {
                    writer.writeObjectSeparator();
                }
            }
            writer.writeObjectClose();
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Void stringNode(StringNode node) {
        try {
            writer.writeString(node.getValue());
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
