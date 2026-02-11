/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.List;

class StartsWithFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "starts_with";
    }

    @Override
    public T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(2, functionArguments);
        T subject = functionArguments.get(0).expectString();
        T prefix = functionArguments.get(1).expectString();

        String subjectStr = runtime.asString(subject);
        String prefixStr = runtime.asString(prefix);

        return runtime.createBoolean(subjectStr.startsWith(prefixStr));
    }
}
