/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.Arrays;
import java.util.List;
import software.amazon.smithy.jmespath.RuntimeType;

class ToArrayFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "to_array";
    }

    @Override
    public T abstractApply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        JmespathAbstractRuntime<T> runtime = evaluator.runtime();
        checkArgumentCount(1, functionArguments);
        T value = functionArguments.get(0).expectValue();

        T isArray = runtime.abstractIs(value, RuntimeType.ARRAY);
        return evaluator.ifThenElse(isArray, value, runtime.arrayBuilder().add(value).build());
    }

    @Override
    public T concreteApply(Evaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        JmespathRuntime<T> runtime = evaluator.runtime();
        checkArgumentCount(1, functionArguments);
        T value = functionArguments.get(0).expectValue();

        if (runtime.is(value, RuntimeType.ARRAY)) {
            return value;
        } else {
            return runtime.arrayBuilder().add(value).build();
        }
    }
}
