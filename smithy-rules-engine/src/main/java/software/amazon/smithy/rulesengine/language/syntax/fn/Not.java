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

import static software.amazon.smithy.rulesengine.language.error.RuleError.context;

import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expression;
import software.amazon.smithy.rulesengine.language.visit.ExpressionVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class Not extends SingleArgFunction<Type.Bool> {
    public static final String ID = "not";

    public Not(FunctionNode functionNode) {
        super(functionNode, Type.bool());
    }

    /**
     * Constructs a Not function with the given expression as argument.
     *
     * @param expression the expression to pass to Not.
     * @return the Not function.
     */
    public static Not ofExpression(Expression expression) {
        return new Not(FunctionNode.ofExpressions(ID, expression));
    }

    /**
     * Returns the expression argument to Not.
     *
     * @return the expression argument.
     */
    public Expression getTarget() {
        return expectOneArgument();
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visitNot(getTarget());
    }

    @Override
    public Type typeCheckLocal(Scope<Type> scope) {
        // Not must be typechecked in a interior scope because information doesn't flow back out of `not`
        return scope.inScope(() -> context("while typechecking `not`", this,
                () -> expectOneArgument().typeCheck(scope).expectBool()));
    }

    @Override
    protected Type typeCheckArgument(Scope<Type> scope, Type.Bool arg) {
        return null;
    }
}
