/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.integrations.core;

import java.util.List;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.traitcodegen.SymbolProperties;
import software.amazon.smithy.traitcodegen.TraitCodegenContext;
import software.amazon.smithy.traitcodegen.TraitCodegenSettings;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
import software.amazon.smithy.traitcodegen.integrations.TraitCodegenIntegration;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.CodeInterceptor;
import software.amazon.smithy.utils.CodeSection;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.SmithyInternalApi;

/**
 * Core integration for Trait code generation.
 *
 * <p>This integration applies no built-in's, but decorates the Symbol provider to replace a
 * shape symbol with a trait symbol definition if the trait has the
 * {@link software.amazon.smithy.model.traits.TraitDefinition} trait applied.
 * Trait symbols are named {@code <ShapeName>Trait} and always have a definition file.
 * This integration runs after all other integrations have finished to ensure that
 * any other type decorators and integrations have already been applied before creating any Trait
 * definitions from the resulting type.
 */
@SmithyInternalApi
public final class CoreIntegration implements TraitCodegenIntegration {

    @Override
    public String name() {
        return "core";
    }

    @Override
    public byte priority() {
        // This integration should be run last, so it picks up the correct
        // base symbol with all other decorators applied.
        return -1;
    }

    @Override
    public SymbolProvider decorateSymbolProvider(
            Model model,
            TraitCodegenSettings settings,
            SymbolProvider symbolProvider
    ) {
        return new SymbolProvider() {
            @Override
            public Symbol toSymbol(Shape shape) {
                if (shape.hasTrait(TraitDefinition.class)) {
                    return getTraitSymbol(settings, shape, symbolProvider.toSymbol(shape));
                }
                return symbolProvider.toSymbol(shape);
            }

            // Necessary to ensure initial toMemberName is not squashed by decorating
            @Override
            public String toMemberName(MemberShape shape) {
                return symbolProvider.toMemberName(shape);
            }
        };
    }

    @Override
    public List<? extends CodeInterceptor<? extends CodeSection, TraitCodegenWriter>> interceptors(
            TraitCodegenContext codegenContext
    ) {
        return ListUtils.of(new JavadocFormatterInterceptor());
    }

    private Symbol getTraitSymbol(TraitCodegenSettings settings, Shape shape, Symbol baseSymbol) {
        String relativeNamespace = TraitCodegenUtils.mapNamespace(settings.smithyNamespace(),
                shape.getId().getNamespace(),
                settings.packageName());
        String name = TraitCodegenUtils.getDefaultTraitName(shape);

        // If the base symbol has an unboxed version, use that as the base symbol
        // instead of the Boxed version.
        if (baseSymbol.getProperty(SymbolProperties.UNBOXED_SYMBOL).isPresent()) {
            baseSymbol = baseSymbol.expectProperty(SymbolProperties.UNBOXED_SYMBOL);
        }

        // Maintain all existing properties, but change the namespace and name of the shape
        // and add the base symbol as a property. The references need to be set to empty list
        // to prevent writing as parameterized classes.
        return baseSymbol.toBuilder()
                .name(name)
                .references(ListUtils.of())
                .namespace(relativeNamespace, ".")
                .putProperty(SymbolProperties.BASE_SYMBOL, baseSymbol)
                .definitionFile("./" + relativeNamespace.replace(".", "/") + "/" + name + ".java")
                .build();
    }
}
