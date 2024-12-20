/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import software.amazon.smithy.build.FileManifest;
import software.amazon.smithy.codegen.core.CodegenContext;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Directive that contains a {@link CodegenContext}.
 *
 * @param <C> CodegenContext type.
 * @param <S> Codegen settings type.
 */
public abstract class ContextualDirective<C extends CodegenContext<S, ?, ?>, S> extends Directive<S> {

    private final C context;

    ContextualDirective(C context, ServiceShape service) {
        super(context.model(), context.settings(), service);
        this.context = context;
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
     * @return Gets the FileManifest being written to for code generation.
     */
    public final FileManifest fileManifest() {
        return context.fileManifest();
    }
}
