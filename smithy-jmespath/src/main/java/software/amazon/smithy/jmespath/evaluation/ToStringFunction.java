/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.RuntimeType;

import java.util.List;

class ToStringFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "to_string";
    }

    @Override
    public T abstractApply(JmespathAbstractRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T value = functionArguments.get(0).expectValue();

        return EvaluationUtils.ifThenElse(runtime, functions,
                runtime.abstractIs(value, RuntimeType.STRING),
                value,
                runtime.abstractToString(value));
    }

    @Override
    public T concreteApply(JmespathRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T value = functionArguments.get(0).expectValue();

        if (runtime.is(value, RuntimeType.STRING)) {
            return value;
        } else {
            return runtime.createString(runtime.toString(value));
        }
    }
}
