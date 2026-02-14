package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.JmespathExpression;

import java.util.List;

public class EvalFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "";
    }

    @Override
    public T abstractApply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        JmespathAbstractRuntime<T> runtime = evaluator.runtime();
        checkArgumentCount(2, functionArguments);
        JmespathExpression f = functionArguments.get(0).expectExpression();
        T value = functionArguments.get(1).expectValue();

        return f.evaluate(value, runtime);
    }
}
