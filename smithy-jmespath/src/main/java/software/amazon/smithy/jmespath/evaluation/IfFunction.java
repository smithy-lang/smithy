package software.amazon.smithy.jmespath.evaluation;

import java.util.List;

class IfFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "if";
    }

    @Override
    public T apply(JmespathRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(3, functionArguments);
        T condition = functionArguments.get(0).expectValue();
        T thenValue = functionArguments.get(1).expectValue();
        // TODO: could be optional, defaulting to NULL or true?
        T elseValue = functionArguments.get(2).expectValue();

        if (runtime.isAbstract()) {
            return runtime.either(thenValue, elseValue);
        }

        return runtime.isTruthy(condition) ? thenValue : elseValue;
    }
}
