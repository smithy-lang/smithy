/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.List;

class ToStringFunction implements Function {
    @Override
    public String name() {
        return "to_string";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T value = functionArguments.get(0).expectValue();

        switch (runtime.typeOf(value)) {
            case STRING:
                return value;
            default:
                return runtime.createString(runtime.toString(value));
        }
    }
}
