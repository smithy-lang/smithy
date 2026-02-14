/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.List;
import software.amazon.smithy.jmespath.JmespathExpression;

class MapFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "map";
    }

    @Override
    public T abstractApply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        JmespathAbstractRuntime<T> runtime = evaluator.runtime();
        checkArgumentCount(2, functionArguments);
        JmespathExpression expression = functionArguments.get(0).expectExpression();
        T array = functionArguments.get(1).expectArray();

        T acc = runtime.arrayBuilder().build();
        return evaluator.foldLeft(
                // TODO: need to insert the expression here
                acc, JmespathExpression.parse("append(acc, eval(&<expression>, element))"), array);
    }

    @Override
    public T concreteApply(Evaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        JmespathRuntime<T> runtime = evaluator.runtime();
        checkArgumentCount(2, functionArguments);
        JmespathExpression expression = functionArguments.get(0).expectExpression();
        T array = functionArguments.get(1).expectArray();

        JmespathRuntime.ArrayBuilder<T> builder = runtime.arrayBuilder();
        for (T element : runtime.asIterable(array)) {
            builder.add(expression.evaluate(element, runtime));
        }
        return builder.build();
    }
}
