/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.expressions.functions;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * An abstract definition of a rule-engine function.
 */
@SmithyUnstableApi
public interface FunctionDefinition {
    /**
     * The ID of this function.
     * @return The ID string
     */
    String getId();

    /**
     * The arguments to this function.
     * @return The function arguments
     */
    List<Type> getArguments();

    /**
     * The return type of this function definition.
     * @return The function return type
     */
    Type getReturnType();

    /**
     * Evaluate the arguments to a given function to compute a result.
     * @return The resulting value
     */
    Value evaluate(List<Value> arguments);

    /**
     * Creates a {@link LibraryFunction} implementation from the given {@link FunctionNode}.
     *
     * @param functionNode the function node to deserialize.
     * @return the created LibraryFunction implementation.
     */
    LibraryFunction createFunction(FunctionNode functionNode);

    static Function<FunctionNode, Optional<LibraryFunction>> createFunctionFactory(
            Iterable<FunctionDefinition> functionDefinitions
    ) {
        // Copy from the provided iterator to prevent issues with potentially
        // caching a ServiceLoader using a Thread's context ClassLoader VM-wide.
        List<FunctionDefinition> definitionsList = new ArrayList<>();
        functionDefinitions.forEach(definitionsList::add);
        return node -> {
            for (FunctionDefinition definition : definitionsList) {
                if (definition.getId().equals(node.getName())) {
                    return Optional.of(definition.createFunction(node));
                }
            }
            return Optional.empty();
        };
    }

    static Function<FunctionNode, Optional<LibraryFunction>> createFunctionFactory() {
        return createFunctionFactory(ServiceLoader.load(FunctionDefinition.class));
    }

    static Function<FunctionNode, Optional<LibraryFunction>> createFunctionFactory(
            ClassLoader classLoader
    ) {
        return createFunctionFactory(ServiceLoader.load(FunctionDefinition.class, classLoader));
    }
}
