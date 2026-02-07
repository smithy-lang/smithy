/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

class AppendFunction implements Function {
    @Override
    public String name() {
        return "append";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(2, functionArguments);
        T array = functionArguments.get(0).expectArray();
        T value = functionArguments.get(1).expectValue();

        JmespathRuntime.ArrayBuilder<T> builder = runtime.arrayBuilder();
        builder.addAll(array);
        builder.add(value);
        return builder.build();
    }
}
