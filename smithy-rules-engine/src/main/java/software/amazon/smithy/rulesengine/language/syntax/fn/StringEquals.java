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

package software.amazon.smithy.rulesengine.language.syntax.fn;

import static software.amazon.smithy.rulesengine.language.error.RuleError.ctx;

import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expr;
import software.amazon.smithy.rulesengine.language.visit.FnVisitor;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class StringEquals extends Fn {
    public static final String ID = "stringEquals";

    public StringEquals(FnNode fnNode) {
        super(fnNode);
    }

    public static StringEquals ofExprs(Expr expr, Expr of) {
        return new StringEquals(FnNode.ofExprs(ID, expr, of));
    }

    @Override
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitStringEquals(this);
    }

    public Expr getLeft() {
        return expectTwoArgs().left;
    }

    public Expr getRight() {
        return expectTwoArgs().right;
    }

    @Override
    protected Type typecheckLocal(Scope<Type> scope) {
        return ctx("while typechecking stringEquals", this, () -> {
            Pair<Expr, Expr> args = expectTwoArgs();
            ctx("in the first argument", args.left, () -> args.left.typecheck(scope).expectString());
            ctx("in the second argument", args.right, () -> args.right.typecheck(scope).expectString());
            return Type.bool();
        });
    }

    @Override
    public Value eval(Scope<Value> scope) {
        Pair<Expr, Expr> args = expectTwoArgs();
        return Value.bool(args.left.eval(scope).expectString().equals(args.right.eval(scope).expectString()));
    }
}
