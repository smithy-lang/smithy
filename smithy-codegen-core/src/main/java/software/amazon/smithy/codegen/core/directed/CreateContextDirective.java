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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.codegen.core.CodegenContext;
import software.amazon.smithy.codegen.core.SmithyIntegration;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.knowledge.ServiceIndex;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;

/**
 * Directive used to create a {@link CodegenContext}.
 *
 * @param <S> Codegen settings type.
 * @param <I> {@link SmithyIntegration} type.

 * @see DirectedCodegen#createContext
 */
public final class CreateContextDirective<S, I extends SmithyIntegration<S, ?, ?>> extends Directive<S> {

    private final SymbolProvider symbolProvider;
    private final FileManifest fileManifest;
    private final List<I> integrations;

    CreateContextDirective(
            Model model,
            S settings,
            ServiceShape service,
            SymbolProvider symbolProvider,
            FileManifest fileManifest,
            List<I> integrations
    ) {
        super(model, settings, service);
        this.symbolProvider = symbolProvider;
        this.fileManifest = fileManifest;
        this.integrations = Collections.unmodifiableList(integrations);
    }

    /**
     * @return Returns the SymbolProvider used during codegen.
     */
    public SymbolProvider symbolProvider() {
        return symbolProvider;
    }

    /**
     * @return Gets the FileManifest being written to for code generation.
     */
    public FileManifest fileManifest() {
        return fileManifest;
    }


    /**
     * @return Returns the list of Integrations used during codegen.
     */
    public List<I> integrations() {
        return integrations;
    }

    /**
     * Get a map of supported protocols on the service shape in the form of shape ID to
     * the definition of the trait.
     *
     * @return Returns the protocol shape IDs of the service.
     * @see ServiceIndex
     */
    public Map<ShapeId, Trait> supportedProtocols() {
        return ServiceIndex.of(model()).getProtocols(service());
    }
}
