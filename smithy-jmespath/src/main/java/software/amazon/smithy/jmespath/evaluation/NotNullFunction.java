/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.List;
import java.util.ListIterator;

import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExceptionType;
import software.amazon.smithy.jmespath.RuntimeType;

class NotNullFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "not_null";
    }

    @Override
    public T abstractApply(JmespathAbstractRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
        T result = runtime.createNull();
        ListIterator<FunctionArgument<T>> iter = functionArguments.listIterator(functionArguments.size());
        while (iter.hasPrevious()) {
            T value = iter.previous().expectValue();
            result = EvaluationUtils.abstractIfThenElse(runtime, functions,
                    runtime.abstractIs(value, RuntimeType.NULL), result, value);
        }
        return result;
    }


    @Override
    public T concreteApply(JmespathRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
        if (functionArguments.isEmpty()) {
            throw new JmespathException(JmespathExceptionType.INVALID_ARITY,
                    "Expected at least 1 argument, got 0");
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
