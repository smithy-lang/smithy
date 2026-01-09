/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.HashMap;
import java.util.Map;

final class FunctionRegistry {

    private static final Map<String, Function> BUILTINS = new HashMap<>();

    private static void registerFunction(Function function) {
        if (BUILTINS.put(function.name(), function) != null) {
            throw new IllegalArgumentException("Duplicate function name: " + function.name());
        }
    }

    static {
        registerFunction(new AbsFunction());
        registerFunction(new AvgFunction());
        registerFunction(new CeilFunction());
        registerFunction(new ContainsFunction());
        registerFunction(new EndsWithFunction());
        registerFunction(new FloorFunction());
        registerFunction(new JoinFunction());
        registerFunction(new KeysFunction());
        registerFunction(new LengthFunction());
        registerFunction(new MapFunction());
        registerFunction(new MaxFunction());
        registerFunction(new MergeFunction());
        registerFunction(new MaxByFunction());
        registerFunction(new MinFunction());
        registerFunction(new MinByFunction());
        registerFunction(new NotNullFunction());
        registerFunction(new ReverseFunction());
        registerFunction(new SortFunction());
        registerFunction(new SortByFunction());
        registerFunction(new StartsWithFunction());
        registerFunction(new SumFunction());
        registerFunction(new ToArrayFunction());
        registerFunction(new ToNumberFunction());
        registerFunction(new ToStringFunction());
        registerFunction(new TypeFunction());
        registerFunction(new ValuesFunction());
    }

    static Function lookup(String name) {
        return BUILTINS.get(name);
    }
}
