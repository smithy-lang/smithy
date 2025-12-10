/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.List;

class MaxFunction implements Function {
    @Override
    public String name() {
        return "max";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T array = functionArguments.get(0).expectArray();
        if (runtime.length(array).intValue() == 0) {
            return runtime.createNull();
        }

        T max = null;
        boolean first = true;
        for (T element : runtime.asIterable(array)) {
            if (first) {
                first = false;
                max = element;
            } else if (runtime.compare(element, max) > 0) {
                max = element;
            }
        }
        // max should never be null at this point
        return max;
    }
}
