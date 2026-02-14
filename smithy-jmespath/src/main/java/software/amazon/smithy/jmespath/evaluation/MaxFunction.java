/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.RuntimeType;

import java.util.List;

class MaxFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "max";
    }

    @Override
    public T abstractApply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        JmespathAbstractRuntime<T> runtime = evaluator.runtime();
        return runtime.either(runtime.createAny(RuntimeType.NUMBER),
                runtime.either(runtime.createAny(RuntimeType.STRING),
                        runtime.createNull()));
    }

    @Override
    public T concreteApply(Evaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        JmespathRuntime<T> runtime = evaluator.runtime();
        checkArgumentCount(1, functionArguments);
        T array = functionArguments.get(0).expectArray();
        if (runtime.length(array) == 0) {
            return runtime.createNull();
        }

        T max = null;
        boolean first = true;
        for (T element : runtime.asIterable(array)) {
            if (first) {
                first = false;
                max = element;
            } else if (runtime.compare(element, max) > 0) {
                max = element;
            }
        }
        // max should never be null at this point
        return max;
    }
}
