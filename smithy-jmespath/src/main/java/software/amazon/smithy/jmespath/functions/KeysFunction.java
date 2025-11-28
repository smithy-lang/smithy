package software.amazon.smithy.jmespath.functions;

import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public class KeysFunction implements Function {
    @Override
    public String name() {
        return "keys";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T value = functionArguments.get(0).expectObject();

        JmespathRuntime.ArrayBuilder<T> arrayBuilder = runtime.arrayBuilder();
        arrayBuilder.addAll(value);
        return arrayBuilder.build();
    }
}
