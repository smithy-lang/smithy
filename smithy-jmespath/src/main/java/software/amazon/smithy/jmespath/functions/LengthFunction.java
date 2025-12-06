/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.functions;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

class LengthFunction implements Function {

    private static final Set<RuntimeType> PARAMETER_TYPES = new HashSet<>();
    static {
        PARAMETER_TYPES.add(RuntimeType.STRING);
        PARAMETER_TYPES.add(RuntimeType.ARRAY);
        PARAMETER_TYPES.add(RuntimeType.OBJECT);
    }

    @Override
    public String name() {
        return "length";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T value = functionArguments.get(0).expectAnyOf(PARAMETER_TYPES);

        return runtime.createNumber(runtime.length(value));
    }
}
