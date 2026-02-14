package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.RuntimeType;

import java.util.List;

class AppendIfNotNullFunction<T> implements Function<T> {

    @Override
    public String name() {
        return "append_if_not_null";
    }

    @Override
    public T abstractApply(AbstractEvaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(2, functionArguments);
        T array = functionArguments.get(0).expectArray();
        T value = functionArguments.get(1).expectValue();

        return evaluator.ifThenElse(
                evaluator.runtime().abstractIs(value, RuntimeType.NULL),
                array,
                evaluator.runtime().arrayBuilder().addAll(array).add(value).build());
    }

    @Override
    public T concreteApply(Evaluator<T> evaluator, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(2, functionArguments);
        T array = functionArguments.get(0).expectArray();
        T value = functionArguments.get(1).expectValue();

        if (evaluator.runtime().is(value, RuntimeType.NULL)) {
            return array;
        } else {
            return evaluator.runtime().arrayBuilder().addAll(array).add(value).build();
        }
    }
}
