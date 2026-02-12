/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.RuntimeType;

import java.util.List;

class ValuesFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "values";
    }

    @Override
    public T abstractApply(JmespathAbstractRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
        return runtime.createAny(RuntimeType.ARRAY);
    }

    @Override
    public T concreteApply(JmespathRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T value = functionArguments.get(0).expectObject();

        JmespathRuntime.ArrayBuilder<T> arrayBuilder = runtime.arrayBuilder();
        for (T key : runtime.asIterable(value)) {
            arrayBuilder.add(runtime.value(value, key));
        } ;
        return arrayBuilder.build();
    }
}
