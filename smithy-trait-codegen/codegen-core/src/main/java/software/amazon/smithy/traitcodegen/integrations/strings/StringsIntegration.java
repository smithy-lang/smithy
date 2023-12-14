/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.integrations.strings;

import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.UniqueItemsTrait;
import software.amazon.smithy.traitcodegen.TraitCodegenContext;
import software.amazon.smithy.traitcodegen.TraitCodegenIntegration;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
import software.amazon.smithy.traitcodegen.TraitGeneratorProvider;

/**
 * Handles the special cases related to the use of strings.
 *
 * <p>
 * NOTE: Must be run after all other integrations to ensure any symbol changes
 * applied by other integrations are picked up
 */
public class StringsIntegration implements TraitCodegenIntegration {
    private static final String INTEGRATION_NAME = "string-list-integration";

    @Override
    public String name() {
        return INTEGRATION_NAME;
    }

    @Override
    public TraitGeneratorProvider decorateGeneratorProvider(TraitCodegenContext context,
                                                            TraitGeneratorProvider provider) {
        return shape -> {
            // Handles special casing for StringListShapes
            // If a shape is a list that does not have unique Items trait (which would make it a set)
            // and shape contains only members that resolve to java strings then we can use a special generator.
            if (shape.isListShape()
                    && !shape.hasTrait(UniqueItemsTrait.class)
                    && hasJavaStringMember(shape, context.symbolProvider())
            ) {
                return new StringListTraitGenerator();
            } else if (shape.isStringShape()
                    && TraitCodegenUtils.isJavaString(context.symbolProvider().toSymbol(shape))
            ) {
                return new StringTraitGenerator();
            }

            return provider.getGenerator(shape);
        };
    }

    private boolean hasJavaStringMember(Shape shape, SymbolProvider symbolProvider) {
        return TraitCodegenUtils.isJavaString(symbolProvider.toSymbol(
                shape.asListShape().orElseThrow(RuntimeException::new).getMember()));
    }
}
