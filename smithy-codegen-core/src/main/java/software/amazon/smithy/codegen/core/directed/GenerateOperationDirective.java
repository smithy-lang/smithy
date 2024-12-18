/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import software.amazon.smithy.codegen.core.CodegenContext;
import software.amazon.smithy.model.shapes.OperationShape;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Directive used to generate an operation.
 *
 * @param <C> CodegenContext type.
 * @param <S> Codegen settings type.
 * @see DirectedCodegen#generateOperation
 */
public class GenerateOperationDirective<C extends CodegenContext<S, ?, ?>, S>
        extends ShapeDirective<OperationShape, C, S> {
    GenerateOperationDirective(C context, ServiceShape service, OperationShape shape) {
        super(context, service, shape);
    }
}
