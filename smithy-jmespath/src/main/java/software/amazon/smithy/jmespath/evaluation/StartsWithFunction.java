/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.RuntimeType;

import java.util.List;

class StartsWithFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "starts_with";
    }

    @Override
    public T abstractApply(JmespathAbstractRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
        return runtime.createAny(RuntimeType.BOOLEAN);
    }

    @Override
    public T concreteApply(JmespathRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(2, functionArguments);
        T subject = functionArguments.get(0).expectString();
        T prefix = functionArguments.get(1).expectString();

        String subjectStr = runtime.asString(subject);
        String prefixStr = runtime.asString(prefix);

        return runtime.createBoolean(subjectStr.startsWith(prefixStr));
    }
}
