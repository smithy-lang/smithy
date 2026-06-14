/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;

/**
 * An {@link AstReader} that walks an already-parsed {@link Node} tree.
 */
final class NodeAstReader implements AstReader {

    private Node current;
    private SourceLocation lastKeyLocation = SourceLocation.NONE;
    private final Deque<Frame> frames = new ArrayDeque<>();

    private static final class Frame {
        // Exactly one of these is set.
        final Iterator<Map.Entry<StringNode, Node>> objectMembers;
        final Iterator<Node> arrayElements;

        Frame(Iterator<Map.Entry<StringNode, Node>> objectMembers, Iterator<Node> arrayElements) {
            this.objectMembers = objectMembers;
            this.arrayElements = arrayElements;
        }
    }

    NodeAstReader(Node root) {
        this.current = root;
    }

    @Override
    public Type currentType() {
        switch (current.getType()) {
            case OBJECT:
                return Type.OBJECT;
            case ARRAY:
                return Type.ARRAY;
            case STRING:
                return Type.STRING;
            case NUMBER:
                return Type.NUMBER;
            case BOOLEAN:
                return Type.BOOLEAN;
            case NULL:
            default:
                return Type.NULL;
        }
    }

    @Override
    public SourceLocation currentLocation() {
        return current.getSourceLocation();
    }

    @Override
    public void startObject() {
        frames.push(new Frame(current.expectObjectNode().getMembers().entrySet().iterator(), null));
    }

    @Override
    public String nextKey() {
        Iterator<Map.Entry<StringNode, Node>> it = peek().objectMembers;
        if (!it.hasNext()) {
            frames.pop();
            return null;
        }
        Map.Entry<StringNode, Node> entry = it.next();
        lastKeyLocation = entry.getKey().getSourceLocation();
        current = entry.getValue();
        return entry.getKey().getValue();
    }

    @Override
    public SourceLocation lastKeyLocation() {
        return lastKeyLocation;
    }

    @Override
    public void startArray() {
        frames.push(new Frame(null, current.expectArrayNode().getElements().iterator()));
    }

    private Frame peek() {
        return Objects.requireNonNull(frames.peek(), "Invalid JSON");
    }

    @Override
    public boolean nextElement() {
        Iterator<Node> it = peek().arrayElements;
        if (!it.hasNext()) {
            frames.pop();
            return false;
        }
        current = it.next();
        return true;
    }

    @Override
    public String expectStringValue(String label) {
        if (!current.isStringNode()) {
            throw new SourceException("Expected " + label + " to be a string, but found "
                    + AstReader.describe(currentType()), current);
        }
        return current.expectStringNode().getValue();
    }

    @Override
    public Node readValueAsNode() {
        // subtree already exists.
        return current;
    }

    @Override
    public void skipValue() {
        // Nothing to consume for a node tree
    }

    @Override
    public int depth() {
        return frames.size();
    }

    @Override
    public void skipToDepth(int targetDepth) {
        while (frames.size() > targetDepth) {
            frames.pop();
        }
    }
}
