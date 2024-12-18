/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.ast;

import software.amazon.smithy.jmespath.ExpressionVisitor;
import software.amazon.smithy.jmespath.JmespathExpression;

/**
 * And expression where both sides must return truthy values. The second
 * truthy value becomes the result of the expression.
 *
 * @see <a href="https://jmespath.org/specification.html#and-expressions">And Expressions</a>
 */
public final class AndExpression extends BinaryExpression {

    public AndExpression(JmespathExpression left, JmespathExpression right) {
        this(left, right, 1, 1);
    }

    public AndExpression(JmespathExpression left, JmespathExpression right, int line, int column) {
        super(left, right, line, column);
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitAnd(this);
    }
}
