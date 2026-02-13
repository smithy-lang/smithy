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

    default T apply(JmespathAbstractRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> arguments) {
        if (runtime instanceof JmespathRuntime) {
            return concreteApply((JmespathRuntime<T>)runtime, functions, arguments);
        } else {
            return abstractApply(runtime, functions, arguments);
        }
    }

    T abstractApply(JmespathAbstractRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> arguments);

    default T concreteApply(JmespathRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> arguments) {
        return abstractApply(runtime, functions, arguments);
    }

    // Helpers

    default void checkArgumentCount(int n, List<FunctionArgument<T>> arguments) {
        if (arguments.size() != n) {
            throw new JmespathException(JmespathExceptionType.INVALID_ARITY,
                    String.format("Expected %d arguments, got %d", n, arguments.size()));
        }
    }

    default T apply(JmespathAbstractRuntime<T> runtime, FunctionRegistry<T> functions, T arg0) {
        return apply(runtime, functions, Collections.singletonList(runtime.createFunctionArgument(arg0)));
    }

    default T apply(JmespathAbstractRuntime<T> runtime, FunctionRegistry<T> functions, T arg0, T arg1) {
        return apply(runtime, functions, Arrays.asList(
                runtime.createFunctionArgument(arg0),
                runtime.createFunctionArgument(arg1)));
    }

    default T apply(JmespathAbstractRuntime<T> runtime, FunctionRegistry<T> functions, T arg0, T arg1, T arg2) {
        return apply(runtime, functions, Arrays.asList(
                runtime.createFunctionArgument(arg0),
                runtime.createFunctionArgument(arg1),
                runtime.createFunctionArgument(arg2)));
    }
}
