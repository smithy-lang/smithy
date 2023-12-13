/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;

public final class GenerateTraitDirective {
    private final Shape shape;
    private final Symbol traitSymbol;
    private final Symbol baseSymbol;
    private final SymbolProvider symbolProvider;
    private final TraitCodegenContext context;
    private final Model model;

    GenerateTraitDirective(TraitCodegenContext context, Shape shape) {
        this.shape = shape;
        this.traitSymbol = context.traitSymbolProvider().toSymbol(shape);
        this.baseSymbol = context.symbolProvider().toSymbol(shape);
        this.symbolProvider = context.symbolProvider();
        this.context = context;
        this.model = context.model();
    }

    public Shape shape() {
        return shape;
    }

    public Symbol traitSymbol() {
        return traitSymbol;
    }

    public Symbol baseSymbol() {
        return baseSymbol;
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
