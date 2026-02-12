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
    public T abstractApply(JmespathAbstractRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
        return runtime.createAny(RuntimeType.NUMBER);
    }

    @Override
    public T concreteApply(JmespathRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
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
