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
