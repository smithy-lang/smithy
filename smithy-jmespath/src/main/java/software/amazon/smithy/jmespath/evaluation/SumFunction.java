/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.JmespathExpression;

import java.util.List;

class SumFunction<T> implements Function<T> {

    private static final JmespathExpression EXPRESSION = JmespathExpression.parse("fold_left(`0`, &add([0], [1]), [0])");

    @Override
    public String name() {
        return "sum";
    }

    @Override
    public T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T array = functionArguments.get(0).expectArray();

        if (runtime.isAbstract()) {
            T args = runtime.arrayBuilder().add(array).build();
            return EXPRESSION.evaluate(args, runtime);
        }

        Number sum = 0L;
        for (T element : runtime.asIterable(array)) {
            sum = EvaluationUtils.addNumbers(sum, runtime.asNumber(element));
        }
        return runtime.createNumber(sum);
    }
}
