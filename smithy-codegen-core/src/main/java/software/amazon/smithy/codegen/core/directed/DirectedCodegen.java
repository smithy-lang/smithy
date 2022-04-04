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
 */
public interface DirectedCodegen<C extends CodegenContext<S, ?>, S> {

    /**
     * Create the {@link SymbolProvider} used to map shapes to code symbols.
     *
     * @param directive Directive context data.
     * @return Returns the created SymbolProvider.
     */
    SymbolProvider createSymbolProvider(CreateSymbolProvider<S> directive);

    /**
     * Creates the codegen context object.
     *
     * @param directive Directive context data.
     * @return Returns the created context object used by the rest of the directed generation.
     */
    C createContext(CreateContext<S> directive);

    /**
     * Generates the code needed for a service shape.
     *
     * @param directive Directive to perform.
     */
    void generateService(GenerateService<C, S> directive);

    /**
     * Generates the code needed for a resource shape.
     *
     * @param directive Directive to perform.
     */
    default void generateResource(GenerateResource<C, S> directive) {
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
    void generateStructure(GenerateStructure<C, S> directive);

    /**
     * Generates the code needed for an error structure.
     *
     * @param directive Directive to perform.
     */
    void generateError(GenerateError<C, S> directive);

    /**
     * Generates the code needed for a union shape.
     *
     * @param directive Directive to perform.
     */
    void generateUnion(GenerateUnion<C, S> directive);

    /*
     * TODO: Uncomment in IDL-2.0 branch
     *
     * Generates the code needed for an enum shape.
     *
     * @param directive Directive to perform.
     */
    //void generateEnumShape(GenerateEnumContext<C, S> directive);

    /**
     * Performs any necessary code generation after all shapes are generated,
     * using the created codegen context object before integrations perform
     * customizations.
     *
     * @param directive Directive to perform.
     */
    default void customizeBeforeIntegrations(Customize<C, S> directive) {
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
    default void customizeAfterIntegrations(Customize<C, S> directive) {
        // Does nothing by default.
    }
}
