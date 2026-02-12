/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.List;

class MergeFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "merge";
    }

    @Override
    public T concreteApply(JmespathRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
        return abstractApply(runtime, functions, functionArguments);
    }

    @Override
    public T abstractApply(JmespathAbstractRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
        JmespathRuntime.ObjectBuilder<T> builder = runtime.objectBuilder();

        for (FunctionArgument<T> arg : functionArguments) {
            T object = arg.expectObject();
            builder.putAll(object);
        }

        return builder.build();
    }
}
