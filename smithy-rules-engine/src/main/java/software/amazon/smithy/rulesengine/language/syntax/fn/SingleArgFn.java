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

import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expr;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public abstract class SingleArgFn<T extends Type> extends Fn {
    T expectedType;

    public SingleArgFn(FnNode fnNode, T expectedType) {
        super(fnNode);
        this.expectedType = expectedType;
    }

    public Expr target() {
        return expectOneArg();
    }

    @Override
    public Value eval(Scope<Value> scope) {
        return evalArg(expectOneArg().eval(scope));
    }

    protected abstract Value evalArg(Value arg);

    @Override
    protected Type typecheckLocal(Scope<Type> scope) {
        return ctx("while typechecking " + this.fnNode.getId(), this, () -> {
            Expr arg = expectOneArg();
            Type t = arg.typecheck(scope);
            if (!t.isA(this.expectedType)) {
                throw new SourceException(String.format("Expected %s but found %s", this.expectedType, t), arg);
            }
            return typecheckArg(scope, (T) t);
        });
    }

    protected abstract Type typecheckArg(Scope<Type> scope, T arg);
}
