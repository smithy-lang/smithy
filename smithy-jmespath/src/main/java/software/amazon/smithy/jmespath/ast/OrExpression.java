/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.ast;

import software.amazon.smithy.jmespath.ExpressionVisitor;
import software.amazon.smithy.jmespath.JmespathExpression;

/**
 * Or expression that returns the expression that returns a truthy value.
 *
 * @see <a href="https://jmespath.org/specification.html#or-expressions">Or Expressions</a>
 */
public final class OrExpression extends BinaryExpression {

    public OrExpression(JmespathExpression left, JmespathExpression right) {
        this(left, right, 1, 1);
    }

    public OrExpression(JmespathExpression left, JmespathExpression right, int line, int column) {
        super(left, right, line, column);
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitOr(this);
    }
}
