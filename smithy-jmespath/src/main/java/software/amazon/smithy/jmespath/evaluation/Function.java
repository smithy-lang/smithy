/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExceptionType;
import software.amazon.smithy.jmespath.type.Type;

public interface Function<T> {

    String name();

    T apply(JmespathRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> arguments);

    // Helpers

    default void checkArgumentCount(int n, List<FunctionArgument<T>> arguments) {
        if (arguments.size() != n) {
            throw new JmespathException(JmespathExceptionType.INVALID_ARITY,
                    String.format("Expected %d arguments, got %d", n, arguments.size()));
        }
    }

    default T apply(JmespathRuntime<T> runtime, FunctionRegistry<T> functions, T arg0) {
        return apply(runtime, functions, Collections.singletonList(FunctionArgument.of(runtime, arg0)));
    }

    default T apply(JmespathRuntime<T> runtime, FunctionRegistry<T> functions, T arg0, T arg1) {
        return apply(runtime, functions, Arrays.asList(
                FunctionArgument.of(runtime, arg0),
                FunctionArgument.of(runtime, arg1)));
    }

    default T apply(JmespathRuntime<T> runtime, FunctionRegistry<T> functions, T arg0, T arg1, T arg2) {
        return apply(runtime, functions, Arrays.asList(
                FunctionArgument.of(runtime, arg0),
                FunctionArgument.of(runtime, arg1),
                FunctionArgument.of(runtime, arg2)));
    }
}
