package software.amazon.smithy.jmespath.functions;

import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.List;

public interface Function {

    String name();

    <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> arguments);

    // Helpers

    default <T> void checkArgumentCount(int n, List<FunctionArgument<T>> arguments) {
        if (arguments.size() != n) {
            throw new IllegalArgumentException(String.format("invalid-arity - Expected %d arguments, got %d", n, arguments.size()));
        }
    }
}
