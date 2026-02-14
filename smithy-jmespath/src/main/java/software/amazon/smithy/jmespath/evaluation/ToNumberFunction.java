/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.RuntimeType;

import java.util.List;

class ToNumberFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "to_number";
    }

    @Override
    public T abstractApply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        return evaluator.runtime().createAny(RuntimeType.NUMBER);
    }

    @Override
    public T concreteApply(Evaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        JmespathRuntime<T> runtime = evaluator.runtime();
        checkArgumentCount(1, functionArguments);
        T value = functionArguments.get(0).expectValue();

        switch (runtime.typeOf(value)) {
            case NUMBER:
                return value;
            case STRING:
                try {
                    String str = runtime.asString(value);
                    if (str.contains(".") || str.toLowerCase().contains("e")) {
                        return runtime.createNumber(Double.parseDouble(str));
                    } else {
                        return runtime.createNumber(Long.parseLong(str));
                    }
                } catch (NumberFormatException e) {
                    return runtime.createNull();
                }
            default:
                return runtime.createNull();
        }
    }
}
