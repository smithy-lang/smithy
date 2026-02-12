package software.amazon.smithy.jmespath;


import software.amazon.smithy.jmespath.evaluation.Function;

import java.util.List;

public interface JmespathExtension {

    <T> List<Function<T>> getFunctions();
}
