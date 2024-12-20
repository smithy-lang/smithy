/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.ast;

import java.util.Objects;
import software.amazon.smithy.jmespath.ExpressionVisitor;
import software.amazon.smithy.jmespath.JmespathExpression;

/**
 * A projection that filters values using a comparison.
 *
 * <p>A filter projection executes the left AST expression, expects it to
 * return an array of values, passes each result of the left expression to
 * a {@link ComparatorExpression}, and yields any value from the comparison
 * expression that returns {@code true} to the right AST expression.
 *
 * <p>Note: while this expression does have a comparator expression, it is
 * still considered a binary expression because it has a left hand side and
 * a right hand side.
 *
 * @see <a href="https://jmespath.org/specification.html#filter-expressions">Filter Expressions</a>
 */
public final class FilterProjectionExpression extends BinaryExpression {

    private final JmespathExpression comparison;

    public FilterProjectionExpression(
            JmespathExpression left,
            JmespathExpression comparison,
            JmespathExpression right
    ) {
        this(left, comparison, right, 1, 1);
    }

    public FilterProjectionExpression(
            JmespathExpression left,
            JmespathExpression comparison,
            JmespathExpression right,
            int line,
            int column
    ) {
        super(left, right, line, column);
        this.comparison = comparison;
    }

    public JmespathExpression getComparison() {
        return comparison;
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitFilterProjection(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof FilterProjectionExpression)) {
            return false;
        }
        FilterProjectionExpression that = (FilterProjectionExpression) o;
        return getComparison().equals(that.getComparison())
                && getLeft().equals(that.getLeft())
                && getRight().equals(that.getRight());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getComparison(), getLeft(), getRight());
    }

    @Override
    public String toString() {
        return "FilterProjectionExpression{"
                + "comparison=" + comparison
                + ", left=" + getLeft()
                + ", right=" + getRight() + '}';
    }
}
