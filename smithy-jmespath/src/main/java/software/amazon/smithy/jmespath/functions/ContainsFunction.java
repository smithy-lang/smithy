package software.amazon.smithy.jmespath.functions;

import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExceptionType;
import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.List;

public class ContainsFunction implements Function {
    @Override
    public String name() {
        return "contains";
    }

    @Override
    public <T> T apply(JmespathRuntime<T> runtime, List<FunctionArgument<T>> functionArguments) {
        checkArgumentCount(2, functionArguments);
        T subject = functionArguments.get(0).expectValue();
        T search = functionArguments.get(1).expectValue();
        switch (runtime.typeOf(subject)) {
            case STRING:
                String searchString = runtime.asString(search);
                String subjectString = runtime.asString(subject);
                return runtime.createBoolean(subjectString.contains(searchString));
            case ARRAY:
                for (T item : runtime.toIterable(subject)) {
                    if (runtime.equal(item, search)) {
                        return runtime.createBoolean(true);
                    }
                }
                return runtime.createBoolean(false);
            default:
                throw new JmespathException(JmespathExceptionType.INVALID_TYPE, "contains is not supported for " + runtime.typeOf(subject));
        }
    }
}
