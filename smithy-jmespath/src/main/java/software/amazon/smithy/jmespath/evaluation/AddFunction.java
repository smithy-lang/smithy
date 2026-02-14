package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.RuntimeType;

import java.util.List;

class AddFunction<T> implements Function<T> {

    @Override
    public String name() {
        return "add";
    }

    @Override
    public T abstractApply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        return evaluator.runtime().createAny(RuntimeType.NUMBER);
    }

    @Override
    public T concreteApply(Evaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(2, functionArguments);
        T left = functionArguments.get(0).expectNumber();
        T right = functionArguments.get(1).expectNumber();

        Number result = EvaluationUtils.addNumbers(evaluator.runtime().asNumber(left), evaluator.runtime().asNumber(right));
        return evaluator.runtime().createNumber(result);
    }
}
