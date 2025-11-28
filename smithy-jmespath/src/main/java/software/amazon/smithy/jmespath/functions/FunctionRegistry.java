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
        registerFunction(new KeysFunction());
        registerFunction(new TypeFunction());
        registerFunction(new ValuesFunction());
    }

    public static Function lookup(String name) {
        return builtins.get(name);
    }
}
