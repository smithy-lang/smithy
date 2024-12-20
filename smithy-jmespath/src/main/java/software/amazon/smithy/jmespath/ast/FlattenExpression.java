/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.ast;

import java.util.Objects;
import software.amazon.smithy.jmespath.ExpressionVisitor;
import software.amazon.smithy.jmespath.JmespathExpression;

/**
 * Flattens the wrapped expression into an array.
 *
 * @see <a href="https://jmespath.org/specification.html#flatten-operator">Flatten Operator</a>
 */
public final class FlattenExpression extends JmespathExpression {

    private final JmespathExpression expression;

    public FlattenExpression(JmespathExpression expression) {
        this(expression, 1, 1);
    }

    public FlattenExpression(JmespathExpression expression, int line, int column) {
        super(line, column);
        this.expression = expression;
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitFlatten(this);
    }

    /**
     * Returns the expression being flattened.
     *
     * @return Returns the expression.
     */
    public JmespathExpression getExpression() {
        return expression;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof FlattenExpression)) {
            return false;
        }
        FlattenExpression that = (FlattenExpression) o;
        return expression.equals(that.expression);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression);
    }

    @Override
    public String toString() {
        return "FlattenExpression{expression=" + expression + '}';
    }
}
