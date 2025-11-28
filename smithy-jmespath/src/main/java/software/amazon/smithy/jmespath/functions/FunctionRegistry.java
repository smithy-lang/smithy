package software.amazon.smithy.jmespath.functions;

import java.util.HashMap;
import java.util.Map;

public class FunctionRegistry {

    private static Map<String, Function> builtins = new HashMap<>();

    private static void registerFunction(Function function) {
        builtins.put(function.name(), function);
    }

    static {
        registerFunction(new AbsFunction());
    }

    public static Function lookup(String name) {
        return builtins.get(name);
    }
}
