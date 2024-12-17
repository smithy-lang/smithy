/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen;

import java.util.Objects;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Custom directive that contains contextual information needed
 * to generate a trait class.
 */
@SmithyInternalApi
public final class GenerateTraitDirective {
    private final Shape shape;
    private final Symbol symbol;
    private final SymbolProvider symbolProvider;
    private final TraitCodegenContext context;
    private final Model model;

    GenerateTraitDirective(TraitCodegenContext context, Shape shape) {
        this.context = Objects.requireNonNull(context);
        this.shape = Objects.requireNonNull(shape);
        this.symbol = Objects.requireNonNull(context.symbolProvider().toSymbol(shape));
        this.symbolProvider = Objects.requireNonNull(context.symbolProvider());
        this.model = Objects.requireNonNull(context.model());
    }

    public Shape shape() {
        return shape;
    }

    public Symbol symbol() {
        return symbol;
    }

    public SymbolProvider symbolProvider() {
        return symbolProvider;
    }

    public TraitCodegenContext context() {
        return context;
    }

    public Model model() {
        return model;
    }
}
