/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.functions;

import java.util.List;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

class MinFunction implements Function {
    @Override
    public String name() {
        return "min";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T array = functionArguments.get(0).expectArray();
        if (runtime.length(array).intValue() == 0) {
            return runtime.createNull();
        }

        T min = null;
        boolean first = true;
        for (T element : runtime.toIterable(array)) {
            if (first) {
                first = false;
                min = element;
            } else if (runtime.compare(element, min) < 0) {
                min = element;
            }
        }
        // min should never be null at this point
        return min;
    }
}
