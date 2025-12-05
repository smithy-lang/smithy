/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.functions;

import java.util.List;
import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExceptionType;
import software.amazon.smithy.jmespath.RuntimeType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

class NotNullFunction implements Function {
    @Override
    public String name() {
        return "not_null";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        if (functionArguments.isEmpty()) {
            throw new JmespathException(JmespathExceptionType.INVALID_ARITY,
                    "Expected at least 1 arguments, got 0");
        }
        for (FunctionArgument<T> arg : functionArguments) {
            T value = arg.expectValue();
            if (!runtime.is(value, RuntimeType.NULL)) {
                return value;
            }
        }
        return runtime.createNull();
    }
}
