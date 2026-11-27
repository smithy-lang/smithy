/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

class SortByFunction implements Function {
    @Override
    public String name() {
        return "sort_by";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(2, functionArguments);
        T array = functionArguments.get(0).expectArray();
        FunctionArgument<T> expression = functionArguments.get(1);
        expression.expectExpression();

        List<T> elements = new ArrayList<>();
        for (T element : runtime.asIterable(array)) {
            elements.add(element);
        }

        Collections.sort(elements, (a, b) -> {
            T aValue = expression.evaluateExpression(a);
            T bValue = expression.evaluateExpression(b);
            return runtime.compare(aValue, bValue);
        });

        JmespathRuntime.ArrayBuilder<T> builder = runtime.arrayBuilder();
        for (T element : elements) {
            builder.add(element);
        }
        return builder.build();
    }
}
