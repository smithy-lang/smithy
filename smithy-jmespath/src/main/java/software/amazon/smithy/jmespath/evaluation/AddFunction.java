package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.RuntimeType;

import java.util.List;

class AddFunction<T> implements Function<T> {

    @Override
    public String name() {
        return "add";
    }

    @Override
    public T apply(JmespathRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(2, functionArguments);
        T left = functionArguments.get(0).expectNumber();
        T right = functionArguments.get(1).expectNumber();

        if (runtime.isAbstract()) {
            return runtime.createAny(RuntimeType.NUMBER);
        }

        Number result = EvaluationUtils.addNumbers(runtime.asNumber(left), runtime.asNumber(right));
        return runtime.createNumber(result);
    }
}
