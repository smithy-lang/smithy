package software.amazon.smithy.jmespath;

import software.amazon.smithy.jmespath.functions.FunctionDefinition;

import java.util.Collections;
import java.util.List;

public interface JmespathExtension {

    /**
     * Provides additional functions.
     *
     * @return A list of functions this extension provides.
     */
    default List<FunctionDefinition> getLibraryFunctions() {
        return Collections.emptyList();
    }
}
