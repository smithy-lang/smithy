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
    /**
     * Visits a literal.
     *
     * @param literal the literal to visit.
     * @return the value from the visitor.
     */
    R visitLiteral(Literal literal);

    /**
     * Visits a reference.
     *
     * @param reference the reference to visit.
     * @return the value from the visitor.
     */
    R visitRef(Reference reference);

    /**
     * Visits a GetAttr function.
     *
     * @param getAttr the GetAttr function to visit.
     * @return the value from the visitor.
     */
    R visitGetAttr(GetAttr getAttr);

    /**
     * Visits an isSet function.
     *
     * @param fn the isSet function to visit.
     * @return the value from the visitor.
     */
    R visitIsSet(Expression fn);

    /**
     * Visits a not function.
     *
     * @param not the not function to visit.
     * @return the value from the visitor.
     */
    R visitNot(Expression not);

    /**
     * Does a boolean equality check.
     *
     * @param left the first value to compare.
     * @param right the second value to compare.
     * @return the value from the visitor.
     */
    R visitBoolEquals(Expression left, Expression right);

    /**
     * Does a string equality check.
     *
     * @param left the first value to compare.
     * @param right the second value to compare.
     * @return the value from the visitor.
     */
    R visitStringEquals(Expression left, Expression right);

    /**
     * Visits a library function.
     *
     * @param fn the library function to visit.
     * @param args the arguments to the function being visited.
     * @return the value from the visitor.
     */
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
