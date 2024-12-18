/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.ast;

import software.amazon.smithy.jmespath.ExpressionVisitor;
import software.amazon.smithy.jmespath.JmespathExpression;

/**
 * Iterates over each element in the array returned from the left expression,
 * passes it to the right expression, and returns the aggregated results.
 *
 * <p>This AST node is created when parsing expressions like {@code [*]},
 * {@code []}, and {@code [1:1]}.
 *
 * @see <a href="https://jmespath.org/specification.html#wildcard-expressions">Wildcard Expressions</a>
 */
public class ProjectionExpression extends BinaryExpression {

    public ProjectionExpression(JmespathExpression left, JmespathExpression right) {
        this(left, right, 1, 1);
    }

    public ProjectionExpression(JmespathExpression left, JmespathExpression right, int line, int column) {
        super(left, right, line, column);
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitProjection(this);
    }
}
