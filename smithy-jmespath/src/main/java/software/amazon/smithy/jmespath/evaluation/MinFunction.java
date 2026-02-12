/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.RuntimeType;

import java.util.List;

class MinFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "min";
    }

    @Override
    public T abstractApply(JmespathAbstractRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
        return runtime.createAny(RuntimeType.NUMBER);
    }

    @Override
    public T concreteApply(JmespathRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T array = functionArguments.get(0).expectArray();
        if (runtime.length(array) == 0) {
            return runtime.createNull();
        }

        T min = null;
        boolean first = true;
        for (T element : runtime.asIterable(array)) {
            if (first) {
                first = false;
                min = element;
            } else if (runtime.compare(element, min) < 0) {
                min = element;
            }
        }
        // min should never be null at this point
        return min;
    }
}
