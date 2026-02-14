/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.List;
import software.amazon.smithy.jmespath.JmespathExpression;

class MaxByFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "max_by";
    }

    @Override
    public T abstractApply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        // TODO: Can do better via fold_left
        return evaluator.createAny();
    }

    @Override
    public T concreteApply(Evaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        JmespathRuntime<T> runtime = evaluator.runtime();
        checkArgumentCount(2, functionArguments);
        T array = functionArguments.get(0).expectArray();
        JmespathExpression expression = functionArguments.get(1).expectExpression();
        if (runtime.length(array) == 0) {
            return runtime.createNull();
        }

        T max = null;
        T maxBy = null;
        boolean first = true;
        for (T element : runtime.asIterable(array)) {
            T by = expression.evaluate(element, runtime);
            if (first) {
                first = false;
                max = element;
                maxBy = by;
            } else if (runtime.compare(by, maxBy) > 0) {
                max = element;
                maxBy = by;
            }
        }
        // max should never be null at this point
        return max;
    }
}
