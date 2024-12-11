/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.ast;

import software.amazon.smithy.jmespath.ExpressionVisitor;
import software.amazon.smithy.jmespath.JmespathExpression;

/**
 * Gets the current node.
 *
 * <a href="https://jmespath.org/specification.html#current-node">current-node</a>
 */
public final class CurrentExpression extends JmespathExpression {

    public CurrentExpression() {
        this(1, 1);
    }

    public CurrentExpression(int line, int column) {
        super(line, column);
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitCurrentNode(this);
    }

    @Override
    public int hashCode() {
        return 1;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CurrentExpression;
    }

    @Override
    public String toString() {
        return "CurrentExpression{}";
    }
}
