/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import software.amazon.smithy.codegen.core.CodegenContext;
import software.amazon.smithy.codegen.core.SmithyIntegration;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.SymbolWriter;
import software.amazon.smithy.codegen.core.WriterDelegator;

/**
 * Provides a directed code generation abstraction to make it easier to
 * implement a Smithy code generator for a single service that leverages
 * other abstractions in smithy-codegen-core, including {@link SmithyIntegration},
 * {@link SymbolProvider}, {@link CodegenContext}, {@link SymbolWriter},
 * and {@link WriterDelegator}.
 *
 * @param <C> Smithy {@link CodegenContext} to use in directed methods.
 * @param <S> Settings object passed to directed methods as part of the context.
 * @param <I> {@link SmithyIntegration} type to use in directed methods.
 */
public interface DirectedCodegen<C extends CodegenContext<S, ?, I>, S, I extends SmithyIntegration<S, ?, C>> {

    /**
     * Create the {@link SymbolProvider} used to map shapes to code symbols.
     *
     * @param directive Directive context data.
     * @return Returns the created SymbolProvider.
     */
    SymbolProvider createSymbolProvider(CreateSymbolProviderDirective<S> directive);

    /**
     * Creates the codegen context object.
     *
     * @param directive Directive context data.
     * @return Returns the created context object used by the rest of the directed generation.
     */
    C createContext(CreateContextDirective<S, I> directive);

    /**
     * Generates the code needed for a service shape.
     *
     * @param directive Directive to perform.
     */
    void generateService(GenerateServiceDirective<C, S> directive);

    /**
     * Generates the code needed for a resource shape.
     *
     * @param directive Directive to perform.
     */
    default void generateResource(GenerateResourceDirective<C, S> directive) {
        // Does nothing by default.
    }

    /**
     * Generates the code needed for an operation shape.
     *
     * @param directive Directive to perform.
     */
    default void generateOperation(GenerateOperationDirective<C, S> directive) {
        // Does nothing by default.
    }

    /**
     * Generates the code needed for a structure shape.
     *
     * <p>This method should not be invoked for structures marked with the
     * {@code error} trait.
     *
     * @param directive Directive to perform.
     */
    void generateStructure(GenerateStructureDirective<C, S> directive);

    /**
     * Generates the code needed for an error structure.
     *
     * @param directive Directive to perform.
     */
    void generateError(GenerateErrorDirective<C, S> directive);

    /**
     * Generates the code needed for a union shape.
     *
     * @param directive Directive to perform.
     */
    void generateUnion(GenerateUnionDirective<C, S> directive);

    /**
     * Generates any code needed for a list shape.
     *
     * @param directive Directive to perform.
     */
    default void generateList(GenerateListDirective<C, S> directive) {}

    /**
     * Generates any code needed for a map shape.
     *
     * @param directive Directive to perform.
     */
    default void generateMap(GenerateMapDirective<C, S> directive) {}

    /**
     * Generates the code needed for an enum shape, whether it's a string shape
     * marked with the enum trait, or a proper enum shape introduced in Smithy
     * IDL 2.0.
     *
     * @param directive Directive to perform.
     */
    void generateEnumShape(GenerateEnumDirective<C, S> directive);

    /**
     * Generates the code needed for an intEnum shape.
     *
     * @param directive Directive to perform.
     */
    void generateIntEnumShape(GenerateIntEnumDirective<C, S> directive);

    /**
     * Performs any necessary code generation before all shapes are generated,
     * using the created codegen context object.
     *
     * @param directive Directive to perform.
     */
    default void customizeBeforeShapeGeneration(CustomizeDirective<C, S> directive) {
        // Does nothing by default.
    }

    /**
     * Performs any necessary code generation after all shapes are generated,
     * using the created codegen context object before integrations perform
     * customizations.
     *
     * @param directive Directive to perform.
     */
    default void customizeBeforeIntegrations(CustomizeDirective<C, S> directive) {
        // Does nothing by default.
    }

    /**
     * Performs any necessary code generation after all shapes are generated,
     * using the created codegen context object after all integrations have
     * performed customizations.
     *
     * <p>This method should be used to do things like:
     *
     * <ul>
     *     <li>Flush any code writers created by your WriterDelegator.</li>
     *     <li>Generate dependency manifests (e.g., poms) from WriterDelegator.</li>
     *     <li>Perform any remaining codegen tasks like formatting or validating the generated output.</li>
     * </ul>
     *
     * @param directive Directive to perform.
     */
    default void customizeAfterIntegrations(CustomizeDirective<C, S> directive) {
        // Does nothing by default.
    }
}
