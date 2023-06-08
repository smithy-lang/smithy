/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
