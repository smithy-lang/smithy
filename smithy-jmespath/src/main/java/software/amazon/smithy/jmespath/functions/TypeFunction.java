package software.amazon.smithy.jmespath.functions;

import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.List;

public class TypeFunction implements Function {
    @Override
    public String name() {
        return "type";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T value = functionArguments.get(0).expectValue();
        return runtime.createString(runtime.typeOf(value).toString());
    }
}
