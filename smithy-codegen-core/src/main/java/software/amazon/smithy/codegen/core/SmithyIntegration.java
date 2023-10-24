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

package software.amazon.smithy.codegen.core;

import java.util.Collections;
import java.util.List;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.utils.AbstractCodeWriter;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * This interface provides the base concept of an "Integration" to
 * Smithy code generators.
 *
 * <p>This class provides the bare minimum that most Smithy code generators
 * can implement as a tool to customize generators. Code generators are
 * expected to extend this interface to add various hooks to their generator
 * (e.g., register protocol generators, register auth scheme integrations,
 * attach middleware, intercept and augment CodeWriter sections, etc).
 *
 * <p>This interface is currently unstable as more requirements
 * may be added in the future.
 *
 * @param <S> The settings object used to configure the generator.
 * @param <W> The type of AbstractCodeWriter used by the generator.
 * @param <C> The CodegenContext value used by the generator.
 */
@SmithyUnstableApi
public interface SmithyIntegration<S, W extends SymbolWriter<W, ?>, C extends CodegenContext<S, W, ?>> {
    /**
     * Gets the name of the integration.
     *
     * <p>This name is referred to when ordering the graph of integrations. The
     * name defaults to the canonical class name if not overridden.
     *
     * @return Returns the integration name.
     */
    default String name() {
        return getClass().getCanonicalName();
    }

    /**
     * Gets the priority ordering relative to the topologically ordered integration graph
     * determined by {@link #runBefore()} and {@link #runAfter()}.
     *
     * <p>Higher numbers come before lower numbers.
     *
     * <p>When ordering, implementations must not allow cycles, and no two integrations
     * may have the same name.
     *
     * @return Returns the priority order.
     */
    default byte priority() {
        return 0;
    }

    /**
     * Configures the integration.
     *
     * <p>This provides access to both the parsed settings for the generator and
     * an unparsed {@link ObjectNode} containing settings for this particular
     * integration.
     *
     * <p>The following {@code smithy-build.json} file contains an example of how
     * this configuration will be set.
     *
     * <pre>{@code
     * {
     *     "version": "1.0",
     *     "projections": {
     *         "codegen-projection": {
     *             "plugins": {
     *                 "code-generator": {
     *                     "service": "com.example#DocumentedService",
     *                     "integrations": {
     *                         "my-integration": {
     *                             "example-setting": "foo"
     *                         }
     *                     }
     *                 }
     *             }
     *         }
     *     }
     * }
     * }</pre>
     *
     * <p>In this example, an integration whose {@link #name} is {@code my-integration}
     * Would receive the extra settings from the key of the same name within the
     * {@code integrations} node.
     *
     * <p>Integrations SHOULD use modeled traits as much as possible to drive
     * configuration. This is intended for configuration that doesn't make sense
     * as a trait, such as configuring a documentation theme.
     *
     * @param settings Settings used to generate code.
     * @param integrationSettings Settings used to configure integrations.
     */
    default void configure(S settings, ObjectNode integrationSettings) {}

    /**
     * Gets the names of integrations that this integration must come before.
     *
     * <p>Dependencies are soft. Dependencies on integration names that cannot be found
     * log a warning and are ignored.
     *
     * @return Returns the integration names this must come before.
     */
    default List<String> runBefore() {
        return Collections.emptyList();
    }

    /**
     * Gets the name of the integrations that this integration must come after.
     *
     * <p>Dependencies are soft. Dependencies on integration names that cannot be found
     * log a warning and are ignored.
     *
     * @return Returns the integration names this must come after.
     */
    default List<String> runAfter() {
        return Collections.emptyList();
    }

    /**
     * Preprocess the model before code generation.
     *
     * <p>This can be used to remove unsupported features, remove traits
     * from shapes (e.g., make members optional), etc.
     *
     * <p>By default, this method will return the given {@code model} as-is.
     *
     * @param model Model being generated.
     * @param settings Settings used to generate code.
     * @return Returns the updated model.
     */
    default Model preprocessModel(Model model, S settings) {
        return model;
    }

    /**
     * Updates the {@link SymbolProvider} used when generating code.
     *
     * <p>This can be used to customize the names of shapes, the package
     * that code is generated into, add dependencies, add imports, etc.
     *
     * <p>By default, this method will return the given {@code symbolProvider}
     * as-is.
     *
     * <p>This integration method should be called only after {@link #preprocessModel}.
     *
     * @param model Model being generated.
     * @param settings Settings used to generate.
     * @param symbolProvider The original {@code SymbolProvider}.
     * @return The decorated {@code SymbolProvider}.
     */
    default SymbolProvider decorateSymbolProvider(Model model, S settings, SymbolProvider symbolProvider) {
        return symbolProvider;
    }

    /**
     * Gets a list of {@link CodeInterceptor}s to register with the {@link AbstractCodeWriter}s
     * created by the code generator.
     *
     * <p>This integration method should be called only after {@link #preprocessModel}
     * and {@link #decorateSymbolProvider}.
     *
     * @param codegenContext Code generation context that can be queried when creating interceptors.
     * @return Returns the list of {@link CodeInterceptor}s.
     */
    default List<? extends CodeInterceptor<? extends CodeSection, W>> interceptors(C codegenContext) {
        return Collections.emptyList();
    }

    /**
     * Allows generators to write additional files, perform additional tasks, and
     * interact directly with a {@link FileManifest} used to write files to the plugin's
     * output.
     *
     * <p>This method should generally be invoked at the end of the code generation process.
     *
     * @param codegenContext Code generation context that can be queried when writing additional files.
     */
    default void customize(C codegenContext) {
        // Does nothing by default.
    }

    /**
     * Topologically sorts a list of integrations based on priority,
     * runBefore, and runAfter, and integration names.
     *
     * @param integrations Integrations to sort.
     * @param <I> The type of integration to sort.
     * @return Returns the sorted integrations.
     * @throws IllegalArgumentException If a cycle is found between integrations.
     * @throws IllegalArgumentException If multiple integrations share the same name.
     */
    static <I extends SmithyIntegration<?, ?, ?>> List<I> sort(Iterable<I> integrations) {
        return new IntegrationTopologicalSort<>(integrations).sort();
    }
}
