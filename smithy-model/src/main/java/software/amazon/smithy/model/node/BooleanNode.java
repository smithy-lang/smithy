/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node;

import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.smithy.model.SourceLocation;

/**
 * Represents a boolean node.
 */
public final class BooleanNode extends Node {
    private final boolean value;

    public BooleanNode(boolean value, SourceLocation sourceLocation) {
        super(sourceLocation);
        this.value = value;
    }

    /**
     * Gets the true or false value of the boolean node.
     *
     * @return Returns true or false.
     */
    public boolean getValue() {
        return value;
    }

    @Override
    public NodeType getType() {
        return NodeType.BOOLEAN;
    }

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        return visitor.booleanNode(this);
    }

    @Override
    public BooleanNode expectBooleanNode(String errorMessage) {
        return this;
    }

    @Override
    public BooleanNode expectBooleanNode(Supplier<String> errorMessage) {
        return this;
    }

    @Override
    public Optional<BooleanNode> asBooleanNode() {
        return Optional.of(this);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof BooleanNode && value == ((BooleanNode) other).value;
    }

    @Override
    public int hashCode() {
        return getType().hashCode() * 7 + (value ? 1 : 0);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
