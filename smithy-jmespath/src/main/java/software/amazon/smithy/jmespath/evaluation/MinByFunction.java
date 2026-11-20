/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.List;

class MinByFunction implements Function {
    @Override
    public String name() {
        return "min_by";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(2, functionArguments);
        T array = functionArguments.get(0).expectArray();
        FunctionArgument<T> expression = functionArguments.get(1);
        expression.expectExpression();
        if (runtime.length(array) == 0) {
            return runtime.createNull();
        }

        T min = null;
        T minBy = null;
        boolean first = true;
        for (T element : runtime.asIterable(array)) {
            T by = expression.evaluateExpression(element);
            if (first) {
                first = false;
                min = element;
                minBy = by;
            } else if (runtime.compare(by, minBy) < 0) {
                min = element;
                minBy = by;
            }
        }
        // max should never be null at this point
        return min;
    }
}
