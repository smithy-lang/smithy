/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.evaluation.value;

import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;

/**
 * An empty value.
 */
public final class EmptyValue extends Value {
    public EmptyValue() {
        super(SourceLocation.none());
    }

    @Override
    public Type getType() {
        return Type.emptyType();
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public Node toNode() {
        return Node.nullNode();
    }

    @Override
    public String toString() {
        return "<empty>";
    }
}
