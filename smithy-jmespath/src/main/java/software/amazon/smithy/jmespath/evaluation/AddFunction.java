package software.amazon.smithy.jmespath.evaluation;

import java.util.List;

class AddFunction implements Function {

    @Override
    public String name() {
        return "add";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(2, functionArguments);
        T left = functionArguments.get(0).expectNumber();
        T right = functionArguments.get(1).expectNumber();
        Number result = EvaluationUtils.addNumbers(runtime.asNumber(left), runtime.asNumber(right));
        return runtime.createNumber(result);
    }
}
