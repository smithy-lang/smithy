/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.codegen.core.directed;

import software.amazon.smithy.codegen.core.CodegenContext;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.shapes.ServiceShape;
import software.amazon.smithy.model.shapes.Shape;

/**
 * Directive used to generate a specific shape.
 *
 * @param <T> Type of shape being generated.
 * @param <C> CodegenContext type.
 * @param <S> Codegen settings type.
 */
public abstract class ShapeDirective<T extends Shape, C extends CodegenContext<S, ?, ?>, S>
        extends ContextualDirective<C, S> {

    private final T shape;
    private final Symbol symbol;

    ShapeDirective(C context, ServiceShape service, T shape) {
        super(context, service);
        this.shape = shape;
        this.symbol = symbolProvider().toSymbol(shape);
    }

    /**
     * Gets the shape being generated.
     *
     * @return Returns the shape to generate.
     */
    public final T shape() {
        return shape;
    }

    /**
     * Gets the symbol created for the shape.
     *
     * <p>This is equivalent to calling {@code symbolProvider().toSymbol(shape())}.
     *
     * @return Returns the shape's symbol.
     */
    public final Symbol symbol() {
        return symbol;
    }
}
