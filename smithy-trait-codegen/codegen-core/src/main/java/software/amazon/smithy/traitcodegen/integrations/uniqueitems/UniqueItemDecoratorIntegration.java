/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.integrations.uniqueitems;

import java.util.List;
import java.util.Set;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.UniqueItemsTrait;
import software.amazon.smithy.traitcodegen.SymbolProperties;
import software.amazon.smithy.traitcodegen.TraitCodegenSettings;
import software.amazon.smithy.traitcodegen.integrations.TraitCodegenIntegration;
import software.amazon.smithy.traitcodegen.utils.SymbolUtil;
import software.amazon.smithy.utils.ListUtils;

public class UniqueItemDecoratorIntegration implements TraitCodegenIntegration {
    private static final String SET_INITIALIZER = "forOrderedSet()";
    private static final String INTEGRATION_NAME = "unique-items-integration";

    @Override
    public String name() {
        return INTEGRATION_NAME;
    }

    @Override
    public List<String> runAfter() {
        return ListUtils.of("id-ref-integration");
    }

    @Override
    public byte priority() {
        // Run before other integrations to make sure symbol changes are picked up
        return 127;
    }

    @Override
    public SymbolProvider decorateSymbolProvider(Model model, TraitCodegenSettings settings,
                                                 SymbolProvider symbolProvider) {
        return shape -> provideSymbol(shape, symbolProvider, model);
    }

    private Symbol provideSymbol(Shape shape, SymbolProvider symbolProvider, Model model) {
        if (shape.isListShape() && shape.hasTrait(UniqueItemsTrait.class)) {
            return SymbolUtil.fromClass(Set.class).toBuilder()
                    .addReference(symbolProvider.toSymbol(shape.asListShape()
                            .orElseThrow(RuntimeException::new).getMember()))
                    .putProperty(SymbolProperties.BUILDER_REF_INITIALIZER, SET_INITIALIZER)
                    .build();
        } else if (shape.isMemberShape()) {
            Shape target = model.expectShape(shape.asMemberShape().orElseThrow(RuntimeException::new).getTarget());
            return provideSymbol(target, symbolProvider, model);
        }
        return symbolProvider.toSymbol(shape);
    }
}
