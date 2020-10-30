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
 * Compares the left and right expression using a comparator,
 * resulting in a boolean value.
 */
public final class ComparisonExpression extends BinaryExpression {

    private final ComparatorType comparator;

    public ComparisonExpression(ComparatorType comparator, JmespathExpression left, JmespathExpression right) {
        this(comparator, left, right, 1, 1);
    }

    public ComparisonExpression(
            ComparatorType comparator,
            JmespathExpression left,
            JmespathExpression right,
            int line,
            int column
    ) {
        super(left, right, line, column);
        this.comparator = comparator;
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitComparison(this);
    }

    /**
     * Gets the comparator to apply to the left and right expressions.
     *
     * @return Returns the comparator.
     */
    public ComparatorType getComparator() {
        return comparator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof ComparisonExpression)) {
            return false;
        }
        ComparisonExpression that = (ComparisonExpression) o;
        return getLeft().equals(that.getLeft())
               && getRight().equals(that.getRight())
               && getComparator().equals(that.getComparator());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getLeft(), getRight(), getComparator());
    }

    @Override
    public String toString() {
        return "ComparatorExpression{comparator='" + getComparator() + '\''
               + ", left=" + getLeft()
               + ", right=" + getRight() + '}';
    }
}
