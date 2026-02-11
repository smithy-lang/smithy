/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExceptionType;

import java.util.HashMap;
import java.util.Map;

public final class FunctionRegistry<T> {

    private final Map<String, Function<T>> functions = new HashMap<>();

    public void registerFunction(Function<T> function) {
        if (functions.put(function.name(), function) != null) {
            throw new IllegalArgumentException("Duplicate function name: " + function.name());
        }
    }

    public Function<T> lookup(String name) {
        return functions.get(name);
    }

    public Function<T> lookup(JmespathRuntime<T> runtime, String name) {
        Function<T> result = runtime.resolveFunction(name);
        if (result != null) {
            return result;
        }

        result = functions.get(name);
        if (result != null) {
            return result;
        }

        throw new JmespathException(JmespathExceptionType.UNKNOWN_FUNCTION, "Unknown function: " + name);
    }
}
