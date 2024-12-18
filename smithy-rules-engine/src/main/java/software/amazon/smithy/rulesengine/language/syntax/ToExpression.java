/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.syntax;

import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Supplies functionality to be coercible into {@link Expression}s for
 * use in composing rule-sets in code.
 */
@SmithyInternalApi
public interface ToExpression {
    /**
     * Convert this into an expression.
     *
     * @return the expression.
     */
    Expression toExpression();
}
