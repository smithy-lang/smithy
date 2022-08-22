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

package software.amazon.smithy.rulesengine.language.lang.fn;

import static software.amazon.smithy.rulesengine.language.error.RuleError.ctx;

import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.lang.expr.Expr;
import software.amazon.smithy.rulesengine.language.lang.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.visit.FnVisitor;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public class BooleanEquals extends Fn {
    public static final String ID = "booleanEquals";

    public BooleanEquals(FnNode fnNode) {
        super(fnNode);
    }

    public static BooleanEquals ofExprs(Expr left, Expr right) {
        return new BooleanEquals(FnNode.ofExprs(ID, left, right));
    }

    public static BooleanEquals fromParam(Parameter param, Expr value) {
        return BooleanEquals.ofExprs(param.expr(), value);
    }

    @Override
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitBoolEquals(this);
    }

    public Expr getLeft() {
        return expectTwoArgs().left;
    }

    public Expr getRight() {
        return expectTwoArgs().right;
    }

    protected Type typecheckLocal(Scope<Type> scope) {
        return ctx("while typechecking booleanEquals", this, () -> {
            Pair<Expr, Expr> args = expectTwoArgs();
            ctx("in the first argument", args.left, () -> args.left.typecheck(scope).expectBool());
            ctx("in the second argument", args.right, () -> args.right.typecheck(scope).expectBool());
            return Type.bool();
        });
    }

    @Override
    public Value eval(Scope<Value> scope) {
        Pair<Expr, Expr> args = expectTwoArgs();
        return ctx("while evaluating boolEquals", this, () ->
                Value.bool(args.left.eval(scope).expectBool() == args.right.eval(scope).expectBool()));
    }
}
