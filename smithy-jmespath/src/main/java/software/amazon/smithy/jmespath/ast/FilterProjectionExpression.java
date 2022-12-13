/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
