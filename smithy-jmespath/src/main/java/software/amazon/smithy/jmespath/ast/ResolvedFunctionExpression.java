package software.amazon.smithy.jmespath.ast;

import software.amazon.smithy.jmespath.ExpressionVisitor;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.jmespath.evaluation.Function;

import java.util.List;

public class ResolvedFunctionExpression extends FunctionExpression {

    private final Function function;

    public ResolvedFunctionExpression(Function function, List<JmespathExpression> arguments) {
        super(function.name(), arguments);
        this.function = function;
    }

    public Function function() {
        return function;
    }
}
