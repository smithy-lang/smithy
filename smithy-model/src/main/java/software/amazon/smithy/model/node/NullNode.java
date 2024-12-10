/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.node;

import java.util.Optional;
import java.util.function.Supplier;
import software.amazon.smithy.model.SourceLocation;

/**
 * Represents a null node.
 */
public final class NullNode extends Node {

    public NullNode(SourceLocation sourceLocation) {
        super(sourceLocation);
    }

    @Override
    public NodeType getType() {
        return NodeType.NULL;
    }

    @Override
    public <R> R accept(NodeVisitor<R> visitor) {
        return visitor.nullNode(this);
    }

    @Override
    public NullNode expectNullNode(String errorMessage) {
        return this;
    }

    @Override
    public NullNode expectNullNode(Supplier<String> errorMessage) {
        return this;
    }

    @Override
    public Optional<NullNode> asNullNode() {
        return Optional.of(this);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof NullNode;
    }

    @Override
    public int hashCode() {
        return getType().hashCode() * 7;
    }

    @Override
    public String toString() {
        return "null";
    }
}
