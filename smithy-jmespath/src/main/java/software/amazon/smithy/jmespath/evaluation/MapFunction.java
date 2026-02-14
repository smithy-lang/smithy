/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.List;
import java.util.Map;

import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.ast.LiteralExpression;

class MapFunction<T> implements Function<T> {

    private static final JmespathExpression FOLDER_TEMPLATE = JmespathExpression.parse("append(acc, eval('mapper', element))");

    @Override
    public String name() {
        return "map";
    }

    @Override
    public T abstractApply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        JmespathAbstractRuntime<T> runtime = evaluator.runtime();
        checkArgumentCount(2, functionArguments);
        JmespathExpression mapper = functionArguments.get(0).expectExpression();
        T array = functionArguments.get(1).expectArray();

        T acc = runtime.arrayBuilder().build();
        JmespathExpression folder = evaluator.substitute(LiteralExpression.from("mapper"), mapper, FOLDER_TEMPLATE);
        return evaluator.foldLeft(acc, folder, array);
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
