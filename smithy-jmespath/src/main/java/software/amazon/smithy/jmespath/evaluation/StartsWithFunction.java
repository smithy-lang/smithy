/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.RuntimeType;

import java.util.List;

class StartsWithFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "starts_with";
    }

    @Override
    public T abstractApply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        return evaluator.runtime().createAny(RuntimeType.BOOLEAN);
    }

    @Override
    public T concreteApply(Evaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(2, functionArguments);
        T subject = functionArguments.get(0).expectString();
        T prefix = functionArguments.get(1).expectString();

        String subjectStr = evaluator.runtime().asString(subject);
        String prefixStr = evaluator.runtime().asString(prefix);

        return evaluator.runtime().createBoolean(subjectStr.startsWith(prefixStr));
    }
}
