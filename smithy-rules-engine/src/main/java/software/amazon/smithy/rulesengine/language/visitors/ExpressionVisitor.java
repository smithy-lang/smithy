/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.rulesengine.language.visitors;

import java.util.List;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.functions.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.functions.GetAttr;
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
