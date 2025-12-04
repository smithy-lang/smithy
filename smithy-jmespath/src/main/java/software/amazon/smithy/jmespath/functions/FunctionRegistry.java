/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.jmespath.functions;

import java.util.HashMap;
import java.util.Map;

public class FunctionRegistry {

    private static Map<String, Function> builtins = new HashMap<>();

    private static void registerFunction(Function function) {
        if (builtins.put(function.name(), function) != null) {
            throw new IllegalArgumentException("Duplicate function name: " + function.name());
        }
    }

    static {
        registerFunction(new AbsFunction());
        registerFunction(new AvgFunction());
        registerFunction(new ContainsFunction());
        registerFunction(new KeysFunction());
        registerFunction(new LengthFunction());
        registerFunction(new SumFunction());
        registerFunction(new TypeFunction());
        registerFunction(new ValuesFunction());
    }

    public static Function lookup(String name) {
        return builtins.get(name);
    }
}
