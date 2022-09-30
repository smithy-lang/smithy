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
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expr.Literal;
import software.amazon.smithy.rulesengine.language.syntax.expr.Reference;
import software.amazon.smithy.rulesengine.language.syntax.fn.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.fn.GetAttr;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * An abstract visitor implementation for a {@link RuleValueVisitor}, and {@link ExpressionVisitor}.
 *
 * @param <R> the return value type.
 */
@SmithyUnstableApi
public abstract class DefaultVisitor<R> implements RuleValueVisitor<R>, ExpressionVisitor<R> {
    public abstract R getDefault();

    @Override
    public R visitLiteral(Literal literal) {
        return getDefault();
    }

    @Override
    public R visitRef(Reference reference) {
        return getDefault();
    }

    @Override
    public R visitIsSet(Expression fn) {
        return getDefault();
    }

    @Override
    public R visitNot(Expression not) {
        return getDefault();
    }

    @Override
    public R visitBoolEquals(Expression left, Expression right) {
        return getDefault();
    }

    @Override
    public R visitStringEquals(Expression left, Expression right) {
        return getDefault();
    }

    public R visitGetAttr(GetAttr getAttr) {
        return getDefault();
    }

    @Override
    public R visitLibraryFunction(FunctionDefinition fn, List<Expression> args) {
        return getDefault();
    }

    @Override
    public R visitTreeRule(List<Rule> rules) {
        return getDefault();
    }

    @Override
    public R visitErrorRule(Expression error) {
        return getDefault();
    }

    @Override
    public R visitEndpointRule(Endpoint endpoint) {
        return getDefault();
    }

}
