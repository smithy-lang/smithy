/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import software.amazon.smithy.codegen.core.CodegenContext;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.StreamingTrait;

/**
 * Directive used to generate a union.
 *
 * @param <C> CodegenContext type.
 * @param <S> Codegen settings type.
 * @see DirectedCodegen#generateUnion
 */
public final class GenerateUnionDirective<C extends CodegenContext<S, ?, ?>, S>
        extends ShapeDirective<UnionShape, C, S> {

    GenerateUnionDirective(C context, ServiceShape service, UnionShape shape) {
        super(context, service, shape);
    }

    /**
     * Check if this is an event stream union.
     *
     * @return Returns true if this is an event stream.
     */
    public boolean isEventStream() {
        return shape().hasTrait(StreamingTrait.class);
    }
}
