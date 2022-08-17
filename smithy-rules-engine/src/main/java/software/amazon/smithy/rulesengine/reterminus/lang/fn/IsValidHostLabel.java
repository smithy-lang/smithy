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

public class IsValidHostLabel extends VarargFn {
    public static final String ID = "isValidHostLabel";

    public IsValidHostLabel(FnNode fnNode) {
        super(fnNode);
    }

    public static IsValidHostLabel ofExprs(Expr expr, boolean allowDots) {
        return new IsValidHostLabel(FnNode.ofExprs(ID, expr, Expr.of(allowDots)));
    }

    @Override
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitIsValidHostLabel(this);
    }

    public Expr hostLabel() {
        return expectTwoArgs().left;
    }

    public Expr allowDots() {
        return expectTwoArgs().right;
    }

    @Override
    public Value eval(Scope<Value> scope) {
        String hostLabel = expectTwoArgs().left.eval(scope).expectString();
        if (allowDots(scope)) {
            return Value.bool(hostLabel.matches("[a-zA-Z\\d][a-zA-Z\\d\\-.]{0,62}"));
        } else {
            return Value.bool(hostLabel.matches("[a-zA-Z\\d][a-zA-Z\\d\\-]{0,62}"));
        }
    }

    protected Type typecheckLocal(Scope<Type> scope) {
        return ctx(
                "while typechecking isValidHostLabel",
                this,
                () -> {
                    allowDots().typecheck(scope).expectBool();
                    hostLabel().typecheck(scope).expectString();
                    return Type.bool();
                }
        );
    }

    private boolean allowDots(Scope<Value> scope) {
        return allowDots().eval(scope).expectBool();
    }
}
