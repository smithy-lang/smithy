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
    public T abstractApply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        JmespathAbstractRuntime<T> runtime = evaluator.runtime();
        checkArgumentCount(1, functionArguments);
        T value = functionArguments.get(0).expectValue();

        return evaluator.ifThenElse(
                runtime.abstractIs(value, RuntimeType.STRING),
                value,
                runtime.abstractToString(value));
    }

    @Override
    public T concreteApply(Evaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        JmespathRuntime<T> runtime = evaluator.runtime();
        checkArgumentCount(1, functionArguments);
        T value = functionArguments.get(0).expectValue();

        if (runtime.is(value, RuntimeType.STRING)) {
            return value;
        } else {
            return runtime.createString(runtime.toString(value));
        }
    }
}
