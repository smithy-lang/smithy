package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.RuntimeType;

import java.util.List;

class AppendIfNotNullFunction<T> implements Function<T> {

    @Override
    public String name() {
        return "add_if_not_null";
    }

    @Override
    public T abstractApply(JmespathAbstractRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(2, functionArguments);
        T array = functionArguments.get(0).expectArray();
        T value = functionArguments.get(1).expectValue();

        return EvaluationUtils.ifThenElse(runtime, functions,
                runtime.abstractIs(value, RuntimeType.NULL),
                array,
                runtime.arrayBuilder().addAll(array).add(value).build());
    }

    @Override
    public T concreteApply(JmespathRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(2, functionArguments);
        T array = functionArguments.get(0).expectArray();
        T value = functionArguments.get(1).expectValue();

        if (runtime.is(value, RuntimeType.NULL)) {
            return array;
        } else {
            return runtime.arrayBuilder().addAll(array).add(value).build();
        }
    }
}
