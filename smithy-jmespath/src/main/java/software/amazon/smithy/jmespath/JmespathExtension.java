package software.amazon.smithy.jmespath;

import software.amazon.smithy.jmespath.evaluation.FunctionRegistry;

public interface JmespathExtension {

    <T> FunctionRegistry<T> getFunctionRegistry();
}
