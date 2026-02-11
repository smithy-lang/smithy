package software.amazon.smithy.jmespath.evaluation;

import java.util.List;

class ConcatFunction<T> implements Function<T> {

    @Override
    public String name() {
        return "concat";
    }

    @Override
    public T apply(JmespathRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(2, functionArguments);
        T left = functionArguments.get(0).expectArray();
        T right = functionArguments.get(1).expectArray();

        return runtime.arrayBuilder().addAll(left).addAll(right).build();
    }
}
