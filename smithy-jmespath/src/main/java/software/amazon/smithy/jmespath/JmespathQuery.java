package software.amazon.smithy.jmespath;


import software.amazon.smithy.jmespath.evaluation.JmespathRuntime;

import java.util.function.Function;

public interface JmespathQuery<T> extends Function<T, T> {

    JmespathRuntime<T> runtime();
}
