/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.expressions;

import java.util.List;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.GetAttr;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Expression visitor pattern.
 *
 * @param <R> Return type of the visitor.
 */
@SmithyUnstableApi
public interface ExpressionVisitor<R> {
    R visitLiteral(Literal literal);

    R visitRef(Reference reference);

    R visitGetAttr(GetAttr getAttr);

    R visitIsSet(Expression fn);

    R visitNot(Expression not);

    R visitBoolEquals(Expression left, Expression right);

    R visitStringEquals(Expression left, Expression right);

    R visitLibraryFunction(FunctionDefinition fn, List<Expression> args);

    abstract class Default<R> implements ExpressionVisitor<R> {
        public abstract R getDefault();

        @Override
        public R visitLiteral(Literal literal) {
            return getDefault();
        }

        @Override
        public R visitRef(Reference reference) {
            return getDefault();
        }

        @Override
        public R visitGetAttr(GetAttr getAttr) {
            return getDefault();
        }

        @Override
        public R visitIsSet(Expression fn) {
            return getDefault();
        }

        @Override
        public R visitNot(Expression not) {
            return getDefault();
        }

        @Override
        public R visitBoolEquals(Expression left, Expression right) {
            return getDefault();
        }

        @Override
        public R visitStringEquals(Expression left, Expression right) {
            return getDefault();
        }

        @Override
        public R visitLibraryFunction(FunctionDefinition fn, List<Expression> args) {
            return getDefault();
        }
    }
}
