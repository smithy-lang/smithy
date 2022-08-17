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

import software.amazon.smithy.rulesengine.reterminus.eval.Scope;
import software.amazon.smithy.rulesengine.reterminus.eval.Type;
import software.amazon.smithy.rulesengine.reterminus.eval.Value;
import software.amazon.smithy.rulesengine.reterminus.lang.expr.Expr;
import software.amazon.smithy.rulesengine.reterminus.lang.expr.Ref;
import software.amazon.smithy.rulesengine.reterminus.visit.ExprVisitor;
import software.amazon.smithy.rulesengine.reterminus.visit.FnVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public class IsSet extends SingleArgFn<Type.Option> {
    public static final String ID = "isSet";


    public IsSet(FnNode fnNode) {
        super(fnNode, Type.optional(new Type.Any()));
    }

    public static IsSet ofExpr(Expr expr) {
        return new IsSet(FnNode.ofExprs(ID, expr));
    }

    @Override
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitIsSet(this);
    }

    @Override
    protected Value evalArg(Value arg) {
        return Value.bool(!arg.isNone());
    }

    @Override
    protected Type typecheckArg(Scope<Type> scope, Type.Option arg) {
        Expr target = target();
        // Insert the non-null fact, but only for refs
        target.accept(new ExprVisitor.Default<Void>() {
            @Override
            public Void getDefault() {
                return null;
            }

            @Override
            public Void visitRef(Ref ref) {
                scope.setNonNull(ref);
                return null;
            }
        });
        return Type.bool();
    }

}
