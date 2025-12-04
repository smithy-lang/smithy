/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.functions;

import java.util.List;
import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

public class ToArrayFunction implements Function {
    @Override
    public String name() {
        return "to_array";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T value = functionArguments.get(0).expectValue();
        
        if (runtime.is(value, RuntimeType.ARRAY)) {
            return value;
        } else {
            JmespathRuntime.ArrayBuilder<T> builder = runtime.arrayBuilder();
            builder.add(value);
            return builder.build();
        }
    }
}