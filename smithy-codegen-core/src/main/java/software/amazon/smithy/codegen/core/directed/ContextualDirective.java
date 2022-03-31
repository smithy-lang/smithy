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
import software.amazon.smithy.codegen.core.CodegenContext;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.SymbolWriter;
import software.amazon.smithy.codegen.core.WriterDelegator;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Directive that contains a {@link CodegenContext}.
 *
 * @param <C> CodegenContext type.
 * @param <S> Codegen settings type.
 */
public abstract class ContextualDirective<C extends CodegenContext<S>, S, D extends WriterDelegator<?>>
        extends Directive<S> {

    private final C context;
    private final D writerDelegator;

    ContextualDirective(C context, ServiceShape service, D writerDelegator) {
        super(context.model(), context.settings(), service);
        this.context = context;
        this.writerDelegator = writerDelegator;
    }

    /**
     * @return Returns the SymbolProvider used during codegen.
     * @see CodegenContext#symbolProvider()
     */
    public final SymbolProvider symbolProvider() {
        return context().symbolProvider();
    }

    /**
     * @return Returns the codegen context object.
     */
    public final C context() {
        return context;
    }

    /**
     * @return Gets the {@link WriterDelegator} used to create {@link SymbolWriter}s.
     */
    public final D writerDelegator() {
        return writerDelegator;
    }

    /**
     * @return Gets the FileManifest being written to for code generation.
     */
    public final FileManifest fileManifest() {
        return context.fileManifest();
    }
}
