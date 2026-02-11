package software.amazon.smithy.jmespath.evaluation;

import software.amazon.smithy.jmespath.JmespathExpression;

import java.util.List;

/**
 * fold_left(0, &(acc + element), [1, 2, 3]) == 6
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
        T collection = functionArguments.get(2).expectValue();

        for (T element : runtime.asIterable(collection)) {
            T fCurrent = runtime.objectBuilder()
                                .put(runtime.createString("acc"), result)
                                .put(runtime.createString("element"), element)
                                .build();
            result = f.evaluate(fCurrent, runtime);
        }
        return result;
    }
}
