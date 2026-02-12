/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.RuntimeType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

class AppendFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "append";
    }

    @Override
    public T abstractApply(JmespathAbstractRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
        return apply(runtime, functions, functionArguments);
    }

    @Override
    public T apply(JmespathAbstractRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(2, functionArguments);
        T array = functionArguments.get(0).expectArray();
        T value = functionArguments.get(1).expectValue();

        return runtime.arrayBuilder().addAll(array).add(value).build();
    }
}
