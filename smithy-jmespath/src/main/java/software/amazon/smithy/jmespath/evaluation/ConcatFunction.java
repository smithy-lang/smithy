package software.amazon.smithy.jmespath.evaluation;

import java.util.List;

class ConcatFunction<T> implements Function<T> {

    @Override
    public String name() {
        return "concat";
    }

    @Override
    public T abstractApply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        return apply(evaluator, functionArguments);
    }

    @Override
    public T apply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(2, functionArguments);
        T left = functionArguments.get(0).expectArray();
        T right = functionArguments.get(1).expectArray();

        return evaluator.runtime().arrayBuilder().addAll(left).addAll(right).build();
    }
}
