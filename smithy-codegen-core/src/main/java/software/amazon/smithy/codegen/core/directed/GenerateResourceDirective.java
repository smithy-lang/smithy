/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import software.amazon.smithy.codegen.core.CodegenContext;
import software.amazon.smithy.model.shapes.ResourceShape;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Directive used to generate a resource.
 *
 * @param <C> CodegenContext type.
 * @param <S> Codegen settings type.
 * @see DirectedCodegen#generateResource
 */
public final class GenerateResourceDirective<C extends CodegenContext<S, ?, ?>, S>
        extends ShapeDirective<ResourceShape, C, S> {
    GenerateResourceDirective(C context, ServiceShape service, ResourceShape shape) {
        super(context, service, shape);
    }
}
