package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExceptionType;
import software.amazon.smithy.jmespath.RuntimeType;

import java.util.List;
import java.util.ListIterator;

class OneNotNullFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "one_not_null";
    }

    @Override
    public T apply(JmespathRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
        if (functionArguments.isEmpty()) {
            throw new JmespathException(JmespathExceptionType.INVALID_ARITY,
                    "Expected at least 1 argument, got 0");
        }

        if (runtime.isAbstract()) {
            T result = runtime.createNull();
            ListIterator<FunctionArgument<T>> iter = functionArguments.listIterator(functionArguments.size());
            while (iter.hasPrevious()) {
                T value = iter.previous().expectValue();
                result = EvaluationUtils.abstractIfThenElse(runtime, functions,
                        runtime.abstractIs(value, RuntimeType.NULL), result, value);
            }
            return result;
        }

        boolean found = false;
        for (FunctionArgument<T> arg : functionArguments) {
            T value = arg.expectValue();
            if (!runtime.is(value, RuntimeType.NULL)) {
                if (found) {
                    return runtime.createBoolean(false);
                } else {
                    found = true;
                }
            }
        }
        return runtime.createBoolean(found);
    }
}
