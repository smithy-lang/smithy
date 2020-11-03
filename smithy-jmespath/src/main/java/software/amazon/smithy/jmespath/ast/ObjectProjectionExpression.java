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
 * A projection of object values.
 *
 * <p>If the left AST expression does not return an object, then the
 * result of the projection is a {@code null} value. Otherwise, the
 * object values are each yielded to the right AST expression,
 * building up a list of results.
 *
 * @see <a href="https://jmespath.org/specification.html#wildcard-expressions">Wildcard Expressions</a>
 */
public final class ObjectProjectionExpression extends ProjectionExpression {

    public ObjectProjectionExpression(JmespathExpression left, JmespathExpression right) {
        this(left, right, 1, 1);
    }

    public ObjectProjectionExpression(JmespathExpression left, JmespathExpression right, int line, int column) {
        super(left, right, line, column);
    }

    @Override
    public <T> T accept(ExpressionVisitor<T> visitor) {
        return visitor.visitObjectProjection(this);
    }
}
