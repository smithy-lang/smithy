/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.List;

class MaxByFunction implements Function {
    @Override
    public String name() {
        return "max_by";
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

        T max = null;
        T maxBy = null;
        boolean first = true;
        for (T element : runtime.asIterable(array)) {
            T by = expression.evaluateExpression(element);
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
