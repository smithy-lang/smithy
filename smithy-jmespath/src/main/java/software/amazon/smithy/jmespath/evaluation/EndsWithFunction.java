/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.RuntimeType;

import java.util.List;

class EndsWithFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "ends_with";
    }

    @Override
    public T abstractApply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        return evaluator.runtime().createAny(RuntimeType.BOOLEAN);
    }

    @Override
    public T concreteApply(Evaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        JmespathRuntime<T> runtime = evaluator.runtime();
        checkArgumentCount(2, functionArguments);
        T subject = functionArguments.get(0).expectString();
        T suffix = functionArguments.get(1).expectString();

        String subjectStr = runtime.asString(subject);
        String suffixStr = runtime.asString(suffix);

        return runtime.createBoolean(subjectStr.endsWith(suffixStr));
    }
}
