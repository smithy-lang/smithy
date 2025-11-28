package software.amazon.smithy.jmespath.functions;

import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.List;

public class ValuesFunction implements Function {
    @Override
    public String name() {
        return "values";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T value = functionArguments.get(0).expectObject();

        JmespathRuntime.ArrayBuilder<T> arrayBuilder = runtime.arrayBuilder();
        for (T key : runtime.toIterable(value)) {
            arrayBuilder.add(runtime.value(value, key));
        };
        return arrayBuilder.build();
    }
}
