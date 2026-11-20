/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.evaluation;

import java.util.HashMap;
import java.util.Map;

final class FunctionRegistry {

    private static final Map<String, Function> BUILTINS = new HashMap<>();

    // Smithy contract extension functions. Not part of the default JMESPath function set:
    // a caller must opt in (the contracts trait does), so other uses of JMESPath such as
    // waiters never resolve them.
    private static final Map<String, Function> MODEL_ONLY = new HashMap<>();

    private static void registerFunction(Function function) {
        if (BUILTINS.put(function.name(), function) != null) {
            throw new IllegalArgumentException("Duplicate function name: " + function.name());
        }
    }

    private static void registerModelOnlyFunction(Function function) {
        if (MODEL_ONLY.put(function.name(), function) != null) {
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

        registerModelOnlyFunction(new RequiresFunction());
        registerModelOnlyFunction(new ResourceFunction());
    }

    static Function lookup(String name) {
        return BUILTINS.get(name);
    }

    static Function lookup(String name, boolean includeModelOnly) {
        Function function = BUILTINS.get(name);
        if (function == null && includeModelOnly) {
            function = MODEL_ONLY.get(name);
        }
        return function;
    }
}
