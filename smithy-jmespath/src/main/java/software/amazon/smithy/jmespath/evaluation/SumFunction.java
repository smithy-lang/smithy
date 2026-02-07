/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.ast.IndexExpression;
import software.amazon.smithy.jmespath.ast.FunctionExpression;

import java.util.Arrays;
import java.util.List;

class SumFunction implements Function {

    private static final JmespathExpression FOLDER = JmespathExpression.parse("add([0], [1])");

    @Override
    public String name() {
        return "sum";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T array = functionArguments.get(0).expectArray();

        if (runtime.isAbstract()) {
            return runtime.foldLeft(runtime.createNumber(0L), FOLDER, array);
        }

        Number sum = 0L;
        for (T element : runtime.asIterable(array)) {
            sum = EvaluationUtils.addNumbers(sum, runtime.asNumber(element));
        }
        return runtime.createNumber(sum);
    }
}
