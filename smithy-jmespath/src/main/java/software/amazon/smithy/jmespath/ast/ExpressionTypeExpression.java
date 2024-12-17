/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.ast;

import java.util.Objects;
import software.amazon.smithy.jmespath.ExpressionVisitor;
import software.amazon.smithy.jmespath.JmespathExpression;

/**
 * Contains a reference to an expression that can be run zero or more
 * times by a function.
 *
 * @see <a href="https://jmespath.org/specification.html#data-types">Data types</a>
 */
public final class ExpressionTypeExpression extends JmespathExpression {

    private final JmespathExpression expression;

    public ExpressionTypeExpression(JmespathExpression expression) {
        this(expression, 1, 1);
    }

    public ExpressionTypeExpression(JmespathExpression expression, int line, int column) {
        super(line, column);
        this.expression = expression;
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitExpressionType(this);
    }

    /**
     * Gets the contained expression.
     *
     * @return Returns the contained expression.
     */
    public JmespathExpression getExpression() {
        return expression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof ExpressionTypeExpression)) {
            return false;
        }
        ExpressionTypeExpression that = (ExpressionTypeExpression) o;
        return expression.equals(that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression);
    }

    @Override
    public String toString() {
        return "ExpressionReferenceExpression{expression=" + expression + '}';
    }
}
