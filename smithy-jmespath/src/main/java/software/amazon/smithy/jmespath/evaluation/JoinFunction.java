/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.List;

class JoinFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "join";
    }

    @Override
    public T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(2, functionArguments);
        String separator = runtime.asString(functionArguments.get(0).expectString());
        T array = functionArguments.get(1).expectArray();

        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (T element : runtime.asIterable(array)) {
            if (!first) {
                result.append(separator);
            }
            result.append(runtime.asString(element));
            first = false;
        }

        return runtime.createString(result.toString());
    }
}
