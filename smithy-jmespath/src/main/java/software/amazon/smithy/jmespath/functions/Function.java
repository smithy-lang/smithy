package software.amazon.smithy.jmespath.functions;

import software.amazon.smithy.jmespath.ast.ExpressionTypeExpression;
import software.amazon.smithy.jmespath.ast.LiteralExpression;
import software.amazon.smithy.jmespath.evaluation.Runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface Function {

    String name();

    <T> T apply(Runtime<T> runtime, List<FunctionArgument<T>> arguments);

    // Helpers

    default <T> void checkArgumentCount(int n, List<FunctionArgument<T>> arguments) {
        if (arguments.size() != n) {
            throw new IllegalArgumentException(String.format("invalid-arity - Expected %d arguments, got %d", n, arguments.size()));
        }
    }
}
