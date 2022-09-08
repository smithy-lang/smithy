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

import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expr;
import software.amazon.smithy.rulesengine.language.syntax.expr.Ref;
import software.amazon.smithy.rulesengine.language.visit.ExprVisitor;
import software.amazon.smithy.rulesengine.language.visit.FnVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class IsSet extends SingleArgFn<Type.Option> {
    public static final String ID = "isSet";


    public IsSet(FnNode fnNode) {
        super(fnNode, Type.optional(new Type.Any()));
    }

    public static IsSet ofExpr(Expr expr) {
        return new IsSet(FnNode.ofExprs(ID, expr));
    }

    @Override
    public <T> T acceptFnVisitor(FnVisitor<T> visitor) {
        return visitor.visitIsSet(this.target());
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
