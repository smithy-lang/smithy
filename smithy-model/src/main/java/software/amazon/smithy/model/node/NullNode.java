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
