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

import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.WriterDelegator;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Directive used to create a {@link WriterDelegator}.
 *
 * @param <S> Codegen settings type.
 * @param <D> Type of {@link WriterDelegator}.
 * @see DirectedCodegen#createContext
 */
public final class CreateWriterDelegator<S, D extends WriterDelegator<?>> extends Directive<S> {

    private final SymbolProvider symbolProvider;
    private final FileManifest fileManifest;

    CreateWriterDelegator(
            Model model,
            S settings,
            ServiceShape service,
            SymbolProvider symbolProvider,
            FileManifest fileManifest
    ) {
        super(model, settings, service);
        this.symbolProvider = symbolProvider;
        this.fileManifest = fileManifest;
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
}
