/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

class ReverseFunction implements Function {
    private static final Set<RuntimeType> PARAMETER_TYPES = new HashSet<>();
    static {
        PARAMETER_TYPES.add(RuntimeType.STRING);
        PARAMETER_TYPES.add(RuntimeType.ARRAY);
    }

    @Override
    public String name() {
        return "reverse";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T value = functionArguments.get(0).expectAnyOf(PARAMETER_TYPES);

        if (runtime.is(value, RuntimeType.STRING)) {
            String str = runtime.asString(value);
            return runtime.createString(new StringBuilder(str).reverse().toString());
        } else {
            List<T> elements = new ArrayList<>();
            for (T element : runtime.toIterable(value)) {
                elements.add(element);
            }
            Collections.reverse(elements);

            JmespathRuntime.ArrayBuilder<T> builder = runtime.arrayBuilder();
            for (T element : elements) {
                builder.add(element);
            }
            return builder.build();
        }
    }
}
