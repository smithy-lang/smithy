package software.amazon.smithy.jmespath.evaluation;

import java.util.List;

class IfFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "if";
    }

    @Override
    public T abstractApply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        T thenValue = functionArguments.get(1).expectValue();
        T elseValue = functionArguments.get(2).expectValue();

        // TODO: Have to pass on any error from the condition
        return evaluator.runtime().either(thenValue, elseValue);
    }

    @Override
    public T concreteApply(Evaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(3, functionArguments);
        T condition = functionArguments.get(0).expectValue();
        T thenValue = functionArguments.get(1).expectValue();
        // TODO: could be optional, defaulting to NULL or true?
        T elseValue = functionArguments.get(2).expectValue();

        return evaluator.runtime().isTruthy(condition) ? thenValue : elseValue;
    }
}
