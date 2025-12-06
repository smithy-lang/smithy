/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.functions;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

class SortFunction implements Function {
    @Override
    public String name() {
        return "sort";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T array = functionArguments.get(0).expectArray();

        List<T> elements = new ArrayList<>();
        for (T element : runtime.asIterable(array)) {
            elements.add(element);
        }

        elements.sort(runtime);

        JmespathRuntime.ArrayBuilder<T> builder = runtime.arrayBuilder();
        for (T element : elements) {
            builder.add(element);
        }
        return builder.build();
    }
}
