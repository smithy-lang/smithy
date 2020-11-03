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

    public Subexpression(JmespathExpression left, JmespathExpression right) {
        this(left, right, 1, 1);
    }

    public Subexpression(JmespathExpression left, JmespathExpression right, int line, int column) {
        super(left, right, line, column);
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitSubexpression(this);
    }
}
