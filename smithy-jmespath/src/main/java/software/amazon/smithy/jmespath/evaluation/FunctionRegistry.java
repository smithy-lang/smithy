/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.HashMap;
import java.util.Map;

public final class FunctionRegistry<T> {

    private final Map<String, Function<T>> functions = new HashMap<>();

    private void registerFunction(Function<T> function) {
        if (functions.put(function.name(), function) != null) {
            throw new IllegalArgumentException("Duplicate function name: " + function.name());
        }
    }

    public FunctionRegistry() {
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

    Function<T> lookup(String name) {
        return functions.get(name);
    }
}
