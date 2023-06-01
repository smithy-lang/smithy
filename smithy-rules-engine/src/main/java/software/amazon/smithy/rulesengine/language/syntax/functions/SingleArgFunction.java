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

import static software.amazon.smithy.rulesengine.language.error.RuleError.context;

import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Represents a rule-set function that expects a single argument.
 * @param <T> the argument type.
 */
@SmithyUnstableApi
abstract class SingleArgFunction<T extends Type> extends Function {
    final T expectedType;

    SingleArgFunction(FunctionNode functionNode, T expectedType) {
        super(functionNode);
        this.expectedType = expectedType;
    }

    public Expression getTarget() {
        return expectOneArgument();
    }

    @Override
    protected Type typeCheckLocal(Scope<Type> scope) {
        return context("while typechecking " + functionNode.getName(), this, () -> {
            Expression arg = expectOneArgument();
            Type type = arg.typeCheck(scope);
            if (!type.isA(expectedType)) {
                throw new SourceException(String.format("Expected %s but found %s", expectedType, type), arg);
            }
            return typeCheckArgument(scope, (T) type);
        });
    }

    protected abstract Type typeCheckArgument(Scope<Type> scope, T arg);
}
