package software.amazon.smithy.jmespath.functions;

import software.amazon.smithy.jmespath.evaluation.EvaluationUtils;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.List;

public class SumFunction implements Function {
    @Override
    public String name() {
        return "sum";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(1, functionArguments);
        T array = functionArguments.get(0).expectArray();
        Number sum = 0L;
        for (T element : runtime.toIterable(array)) {
            sum = EvaluationUtils.addNumbers(sum, runtime.asNumber(element));
        }
        return runtime.createNumber(sum);
    }
}
