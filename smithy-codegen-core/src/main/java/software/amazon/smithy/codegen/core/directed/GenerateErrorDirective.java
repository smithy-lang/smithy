/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import software.amazon.smithy.codegen.core.CodegenContext;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.ErrorTrait;

/**
 * Directive used to generate an error.
 *
 * @param <C> CodegenContext type.
 * @param <S> Codegen settings type.
 * @see DirectedCodegen#generateError
 */
public final class GenerateErrorDirective<C extends CodegenContext<S, ?, ?>, S>
        extends ShapeDirective<StructureShape, C, S> {

    GenerateErrorDirective(C context, ServiceShape service, StructureShape shape) {
        super(context, service, shape);
    }

    /**
     * Gets the {@code error} trait.
     *
     * <p>This is equivalent to calling {@code shape().expectTrait(ErrorTrait.class)}.
     *
     * @return Gets the {@link ErrorTrait} of the error.
     */
    public ErrorTrait errorTrait() {
        return shape().expectTrait(ErrorTrait.class);
    }
}
