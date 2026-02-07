/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.List;
import software.amazon.smithy.jmespath.RuntimeType;

class ToArrayFunction implements Function {
    @Override
    public String name() {
        return "to_array";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T value = functionArguments.get(0).expectValue();

        if (runtime.isAbstract()) {
            T isArray = runtime.abstractIs(value, RuntimeType.ARRAY);
            return runtime.ifThenElse(isArray, value, runtime.arrayBuilder().add(value).build());
        }

        if (runtime.is(value, RuntimeType.ARRAY)) {
            return value;
        } else {
            return runtime.arrayBuilder().add(value).build();
        }
    }
}
