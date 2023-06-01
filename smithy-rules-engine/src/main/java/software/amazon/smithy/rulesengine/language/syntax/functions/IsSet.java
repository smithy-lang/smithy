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

package software.amazon.smithy.rulesengine.language.syntax.functions;

import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.type.OptionalType;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.rulesengine.language.visitors.ExpressionVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A rule-set function for determining whether a reference parameter is set.
 */
@SmithyUnstableApi
public final class IsSet extends SingleArgFunction<OptionalType> {
    public static final String ID = "isSet";

    public IsSet(FunctionNode functionNode) {
        super(functionNode, Type.optionalType(Type.anyType()));
    }

    /**
     * Returns a new IsSet function taking the given expression as an argument.
     *
     * @param expression the argument to IsSet.
     * @return new IsSet instance.
     */
    public static IsSet ofExpression(Expression expression) {
        return new IsSet(FunctionNode.ofExpressions(ID, expression));
    }

    @Override
    protected Type typeCheckArgument(Scope<Type> scope, OptionalType arg) {
        Expression target = getTarget();
        // Insert the non-null fact, but only for refs
        target.accept(new ExpressionVisitor.Default<Void>() {
            @Override
            public Void getDefault() {
                return null;
            }

            @Override
            public Void visitRef(Reference reference) {
                scope.setNonNull(reference);
                return null;
            }
        });
        return Type.booleanType();
    }

    @Override
    public <R> R accept(ExpressionVisitor<R> visitor) {
        return visitor.visitIsSet(getTarget());
    }
}
