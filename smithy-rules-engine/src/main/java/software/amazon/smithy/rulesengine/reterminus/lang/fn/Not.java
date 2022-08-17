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

package software.amazon.smithy.rulesengine.reterminus.lang.fn;

import static software.amazon.smithy.rulesengine.reterminus.error.RuleError.ctx;

import software.amazon.smithy.rulesengine.reterminus.eval.Scope;
import software.amazon.smithy.rulesengine.reterminus.eval.Type;
import software.amazon.smithy.rulesengine.reterminus.eval.Value;
import software.amazon.smithy.rulesengine.reterminus.lang.expr.Expr;
import software.amazon.smithy.rulesengine.reterminus.visit.FnVisitor;

public class Not extends SingleArgFn<Type.Bool> {

    public static final String ID = "not";

    public Not(FnNode fnNode) {
        super(fnNode, Type.bool());
    }

    public static Not ofExpr(Expr expr) {
        return new Not(FnNode.ofExprs(ID, expr));
    }

    public static Not ofExprs(Expr expr) {
        return new Not(FnNode.ofExprs(ID, expr));
    }

    @Override
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitNot(this);
    }

    public Expr target() {
        return expectOneArg();
    }

    @Override
    protected Value evalArg(Value arg) {
        return Value.bool(!arg.expectBool());
    }

    @Override
    public Type typecheckLocal(Scope<Type> scope) {
        // Not must be typechecked in a interior scope because information doesn't flow back out of `not`
        return scope.inScope(() -> ctx("while typechecking `not`", this,
                () -> expectOneArg().typecheck(scope).expectBool()));
    }

    @Override
    protected Type typecheckArg(Scope<Type> scope, Type.Bool arg) {
        return null;
    }
}
