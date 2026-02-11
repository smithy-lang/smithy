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

    // TODO: Set up SPI
    public void addBuiltins() {
        registerFunction(new AbsFunction<>());
        registerFunction(new AvgFunction<>());
        registerFunction(new CeilFunction<>());
        registerFunction(new ContainsFunction<>());
        registerFunction(new EndsWithFunction<>());
        registerFunction(new FloorFunction<>());
        registerFunction(new JoinFunction<>());
        registerFunction(new KeysFunction<>());
        registerFunction(new LengthFunction<>());
        registerFunction(new MapFunction<>());
        registerFunction(new MaxFunction<>());
        registerFunction(new MergeFunction<>());
        registerFunction(new MaxByFunction<>());
        registerFunction(new MinFunction<>());
        registerFunction(new MinByFunction<>());
        registerFunction(new NotNullFunction<>());
        registerFunction(new ReverseFunction<>());
        registerFunction(new SortFunction<>());
        registerFunction(new SortByFunction<>());
        registerFunction(new StartsWithFunction<>());
        registerFunction(new SumFunction<>());
        registerFunction(new ToArrayFunction<>());
        registerFunction(new ToNumberFunction<>());
        registerFunction(new ToStringFunction<>());
        registerFunction(new TypeFunction<>());
        registerFunction(new ValuesFunction<>());
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
