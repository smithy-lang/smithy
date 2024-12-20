/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.ast;

import java.util.Objects;
import software.amazon.smithy.jmespath.JmespathExpression;

/**
 * Abstract class representing expressions that have a left and right side.
 */
public abstract class BinaryExpression extends JmespathExpression {

    private final JmespathExpression left;
    private final JmespathExpression right;

    public BinaryExpression(JmespathExpression left, JmespathExpression right, int line, int column) {
        super(line, column);
        this.left = left;
        this.right = right;
    }

    /**
     * Gets the left side of the expression.
     *
     * @return Returns the expression on the left.
     */
    public final JmespathExpression getLeft() {
        return left;
    }

    /**
     * Gets the right side of the expression.
     *
     * @return Returns the expression on the right.
     */
    public final JmespathExpression getRight() {
        return right;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (this == o) {
            return true;
        } else if (!o.getClass().equals(getClass())) {
            return false;
        }
        BinaryExpression that = (BinaryExpression) o;
        return getLeft().equals(that.getLeft()) && getRight().equals(that.getRight());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass().getSimpleName(), getLeft(), getRight());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{left=" + left + ", right=" + right + '}';
    }
}
