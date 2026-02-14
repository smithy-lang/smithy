/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.List;
import software.amazon.smithy.jmespath.JmespathExpression;

class MinByFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "min_by";
    }

    @Override
    public T abstractApply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        // TODO: Can do better via fold_left
        return evaluator.createAny();
    }

    @Override
    public T concreteApply(Evaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(2, functionArguments);
        T array = functionArguments.get(0).expectArray();
        JmespathExpression expression = functionArguments.get(1).expectExpression();
        if (evaluator.runtime().length(array) == 0) {
            return evaluator.runtime().createNull();
        }

        T min = null;
        T minBy = null;
        boolean first = true;
        for (T element : evaluator.runtime().asIterable(array)) {
            T by = expression.evaluate(element, evaluator.runtime());
            if (first) {
                first = false;
                min = element;
                minBy = by;
            } else if (evaluator.runtime().compare(by, minBy) < 0) {
                min = element;
                minBy = by;
            }
        }
        // max should never be null at this point
        return min;
    }
}
