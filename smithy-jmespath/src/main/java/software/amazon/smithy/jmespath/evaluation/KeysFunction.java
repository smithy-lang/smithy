/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.List;

class KeysFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "keys";
    }

    @Override
    public T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T value = functionArguments.get(0).expectObject();

        return runtime.arrayBuilder().addAll(value).build();
    }
}
