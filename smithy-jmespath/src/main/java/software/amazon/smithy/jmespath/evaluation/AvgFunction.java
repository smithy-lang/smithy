/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.RuntimeType;

import java.util.List;

class AvgFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "avg";
    }

    @Override
    public T abstractApply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        JmespathAbstractRuntime<T> runtime = evaluator.runtime();
        return runtime.either(runtime.createAny(RuntimeType.NUMBER), runtime.createNull());
    }

    @Override
    public T concreteApply(Evaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        JmespathRuntime<T> runtime = evaluator.runtime();
        checkArgumentCount(1, functionArguments);
        T array = functionArguments.get(0).expectArray();
        Number length = runtime.length(array);
        if (length.intValue() == 0) {
            return runtime.createNull();
        }
        Number sum = 0D;
        for (T element : runtime.asIterable(array)) {
            sum = EvaluationUtils.addNumbers(sum, runtime.asNumber(element));
        }
        return runtime.createNumber(EvaluationUtils.divideNumbers(sum, length));
    }
}
