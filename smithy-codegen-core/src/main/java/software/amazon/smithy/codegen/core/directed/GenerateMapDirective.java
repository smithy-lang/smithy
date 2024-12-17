/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import software.amazon.smithy.codegen.core.CodegenContext;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.ServiceShape;

/**
 * Directive used to generate a map.
 *
 * @param <C> CodegenContext type.
 * @param <S> Codegen settings type.
 * @see DirectedCodegen#generateMap
 */
public class GenerateMapDirective<C extends CodegenContext<S, ?, ?>, S>
        extends ShapeDirective<MapShape, C, S> {
    GenerateMapDirective(C context, ServiceShape service, MapShape shape) {
        super(context, service, shape);
    }
}
