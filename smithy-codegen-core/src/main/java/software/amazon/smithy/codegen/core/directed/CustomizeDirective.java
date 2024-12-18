/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import software.amazon.smithy.codegen.core.CodegenContext;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Directive used to perform post-processing code generation.
 *
 * @param <C> CodegenContext type.
 * @param <S> Codegen settings type.
 * @see DirectedCodegen#customizeBeforeIntegrations
 * @see DirectedCodegen#customizeAfterIntegrations
 */
public final class CustomizeDirective<C extends CodegenContext<S, ?, ?>, S> extends ContextualDirective<C, S> {
    CustomizeDirective(C context, ServiceShape service) {
        super(context, service);
    }
}
