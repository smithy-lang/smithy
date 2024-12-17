/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import software.amazon.smithy.codegen.core.CodegenContext;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Directive used to generate a list.
 *
 * @param <C> CodegenContext type.
 * @param <S> Codegen settings type.
 * @see DirectedCodegen#generateList
 */
public class GenerateListDirective<C extends CodegenContext<S, ?, ?>, S>
        extends ShapeDirective<ListShape, C, S> {
    GenerateListDirective(C context, ServiceShape service, ListShape shape) {
        super(context, service, shape);
    }
}
