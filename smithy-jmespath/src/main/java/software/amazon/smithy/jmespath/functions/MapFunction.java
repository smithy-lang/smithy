/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.functions;

import java.util.List;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

public class MapFunction implements Function {
    @Override
    public String name() {
        return "map";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(2, functionArguments);
        JmespathExpression expression = functionArguments.get(0).expectExpression();
        T array = functionArguments.get(1).expectArray();

        JmespathRuntime.ArrayBuilder<T> builder = runtime.arrayBuilder();
        for (T element : runtime.toIterable(array)) {
            builder.add(expression.evaluate(element, runtime));
        }
        return builder.build();
    }
}
