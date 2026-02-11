package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.JmespathExpression;

import java.util.List;

/**
 * fold_left(0, &([0] + [1]), [1, 2, 3]) == 6
 */
class FoldLeftFunction<T> implements Function<T> {
    @Override
    public String name() {
        return "fold_left";
    }

    @Override
    public T apply(JmespathRuntime<T> runtime, FunctionRegistry<T> functions, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(3, functionArguments);
        T result = functionArguments.get(0).expectValue();
        JmespathExpression f = functionArguments.get(1).expectExpression();
        T collection = functionArguments.get(0).expectValue();

        for (T element : runtime.asIterable(collection)) {
            T fCurrent = runtime.arrayBuilder().add(result).add(element).build();
            result = f.evaluate(fCurrent, runtime);
        }
        return result;
    }
}
