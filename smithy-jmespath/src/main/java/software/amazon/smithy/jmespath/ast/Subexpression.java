/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.ast;

import software.amazon.smithy.jmespath.ExpressionVisitor;
import software.amazon.smithy.jmespath.JmespathExpression;

/**
 * Visits the left expression and passes its result to the right expression.
 *
 * <p>This AST node is used for both sub-expressions and pipe-expressions in
 * the JMESPath specification.
 *
 * @see <a href="https://jmespath.org/specification.html#subexpressions">SubExpressions</a>
 * @see <a href="https://jmespath.org/specification.html#pipe-expressions">Pipe expressions</a>
 */
public final class Subexpression extends BinaryExpression {

    private final boolean isPipe;

    public Subexpression(JmespathExpression left, JmespathExpression right) {
        this(left, right, 1, 1);
    }

    public Subexpression(JmespathExpression left, JmespathExpression right, int line, int column) {
        this(left, right, line, column, false);
    }

    public Subexpression(JmespathExpression left, JmespathExpression right, boolean isPipe) {
        this(left, right, 1, 1);
    }

    public Subexpression(JmespathExpression left, JmespathExpression right, int line, int column, boolean isPipe) {
        super(left, right, line, column);
        this.isPipe = isPipe;
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitSubexpression(this);
    }

    /**
     * @return Returns true if this node was created from a pipe "|".
     */
    public boolean isPipe() {
        return isPipe;
    }
}
