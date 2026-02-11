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
    public T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        JmespathRuntime.ObjectBuilder<T> builder = runtime.objectBuilder();

        for (FunctionArgument<T> arg : functionArguments) {
            T object = arg.expectObject();
            builder.putAll(object);
        }

        return builder.build();
    }
}
