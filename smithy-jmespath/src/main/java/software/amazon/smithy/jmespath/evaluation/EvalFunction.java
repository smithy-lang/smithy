package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.JmespathExpression;

import java.util.List;

public class EvalFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "eval";
    }

    @Override
    public T abstractApply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        // TODO: more precise, if the argument types are correct
        return evaluator.createAny();
    }

    @Override
    public T concreteApply(Evaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        JmespathAbstractRuntime<T> runtime = evaluator.runtime();
        checkArgumentCount(2, functionArguments);
        JmespathExpression f = functionArguments.get(0).expectExpression();
        T value = functionArguments.get(1).expectValue();

        return f.evaluate(value, runtime);
    }
}
