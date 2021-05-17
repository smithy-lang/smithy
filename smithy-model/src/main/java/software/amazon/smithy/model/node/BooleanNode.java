/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
