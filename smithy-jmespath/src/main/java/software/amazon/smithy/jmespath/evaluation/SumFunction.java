/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.RuntimeType;

import java.util.List;

class SumFunction<T> implements Function<T> {

    private static final JmespathExpression EXPRESSION = JmespathExpression.parse("fold_left(`0`, &add(acc, element), [0])");

    @Override
    public String name() {
        return "sum";
    }

    @Override
    public T abstractApply(JmespathAbstractRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T array = functionArguments.get(0).expectArray();

        T args = runtime.arrayBuilder().add(array).build();
        return EXPRESSION.evaluate(args, runtime);
    }


    @Override
    public T concreteApply(JmespathRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T array = functionArguments.get(0).expectArray();

        Number sum = 0L;
        for (T element : runtime.asIterable(array)) {
            sum = EvaluationUtils.addNumbers(sum, runtime.asNumber(element));
        }
        return runtime.createNumber(sum);
    }
}
