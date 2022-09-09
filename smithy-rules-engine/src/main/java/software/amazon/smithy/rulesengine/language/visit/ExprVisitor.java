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

package software.amazon.smithy.rulesengine.language.visit;

import java.util.List;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expr;
import software.amazon.smithy.rulesengine.language.syntax.expr.Literal;
import software.amazon.smithy.rulesengine.language.syntax.expr.Ref;
import software.amazon.smithy.rulesengine.language.syntax.fn.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.fn.GetAttr;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public interface ExprVisitor<R> {
    R visitLiteral(Literal literal);

    R visitRef(Ref ref);

    R visitGetAttr(GetAttr getAttr);

    R visitIsSet(Expr fn);

    R visitNot(Expr not);

    R visitBoolEquals(Expr left, Expr right);

    R visitStringEquals(Expr left, Expr right);

    R visitLibraryFunction(FunctionDefinition fn, List<Expr> args);

    abstract class Default<R> implements ExprVisitor<R> {
        public abstract R getDefault();

        @Override
        public R visitLiteral(Literal literal) {
            return getDefault();
        }

        @Override
        public R visitRef(Ref ref) {
            return getDefault();
        }

        public R visitGetAttr(GetAttr getAttr) {
            return getDefault();
        }

        @Override
        public R visitIsSet(Expr fn) {
            return getDefault();
        }

        @Override
        public R visitNot(Expr not) {
            return getDefault();
        }

        @Override
        public R visitBoolEquals(Expr left, Expr right) {
            return getDefault();
        }

        @Override
        public R visitStringEquals(Expr left, Expr right) {
            return getDefault();
        }

        @Override
        public R visitLibraryFunction(FunctionDefinition fn, List<Expr> args) {
            return getDefault();
        }
    }
}
