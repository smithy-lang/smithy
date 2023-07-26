/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.function.Function;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionNode;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.validators.AuthSchemeValidator;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Provides access to endpoint components loaded through {@link EndpointRuleSetExtension}s.
 */
@SmithyInternalApi
public interface EndpointComponentFactory {

    /**
     * Returns true if a built-in of the provided name has been registered.
     *
     * @param name the name of the built-in to check for.
     * @return true if the built-in is present, false otherwise.
     */
    boolean hasBuiltIn(String name);

    /**
     * Gets the built-in names as a joined string.
     *
     * @return a string of the built-in names.
     */
    String getKeyString();

    /**
     * Creates a {@link LibraryFunction} factory function using the loaded function definitions.
     *
     * @return the created factory.
     */
    Function<FunctionNode, Optional<LibraryFunction>> createFunctionFactory();

    /**
     * Gets loaded authentication scheme validators.
     *
     * @return a list of {@link AuthSchemeValidator}s.
     */
    List<AuthSchemeValidator> getAuthSchemeValidators();

    static EndpointComponentFactory createServiceFactory(
            Map<String, Parameter> builtIns,
            Map<String, FunctionDefinition> libraryFunctions,
            List<AuthSchemeValidator> authSchemeValidators
    ) {
        return new EndpointComponentFactory() {
            @Override
            public boolean hasBuiltIn(String name) {
                return builtIns.containsKey(name);
            }

            @Override
            public String getKeyString() {
                return String.join(", ", builtIns.keySet());
            }

            @Override
            public Function<FunctionNode, Optional<LibraryFunction>> createFunctionFactory() {
                return node -> {
                    if (libraryFunctions.containsKey(node.getName())) {
                        return Optional.of(libraryFunctions.get(node.getName()).createFunction(node));
                    }
                    return Optional.empty();
                };
            }

            @Override
            public List<AuthSchemeValidator> getAuthSchemeValidators() {
                return authSchemeValidators;
            }
        };
    }

    static EndpointComponentFactory createServiceFactory(ClassLoader classLoader) {
        Map<String, Parameter> builtIns = new HashMap<>();
        Map<String, FunctionDefinition> libraryFunctions = new HashMap<>();
        List<AuthSchemeValidator> authSchemeValidators = new ArrayList<>();
        for (EndpointRuleSetExtension extension : ServiceLoader.load(EndpointRuleSetExtension.class, classLoader)) {
            String name;
            for (Parameter builtIn : extension.getBuiltIns()) {
                name = builtIn.getBuiltIn().get();
                if (builtIns.containsKey(name)) {
                    throw new RuntimeException("Attempted to load a duplicate built-in parameter: " + name);
                }
                builtIns.put(name, builtIn);
            }

            for (FunctionDefinition functionDefinition : extension.getLibraryFunctions()) {
                name = functionDefinition.getId();
                if (libraryFunctions.containsKey(name)) {
                    throw new RuntimeException("Attempted to load a duplicate library function: " + name);
                }
                libraryFunctions.put(name, functionDefinition);
            }

            authSchemeValidators.addAll(extension.getAuthSchemeValidators());
        }

        return createServiceFactory(builtIns, libraryFunctions, authSchemeValidators);
    }
}
