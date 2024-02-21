/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.integrations.idref;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.codegen.core.SymbolReference;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.IdRefTrait;
import software.amazon.smithy.traitcodegen.Mapper;
import software.amazon.smithy.traitcodegen.SymbolProperties;
import software.amazon.smithy.traitcodegen.TraitCodegenSettings;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
import software.amazon.smithy.traitcodegen.integrations.TraitCodegenIntegration;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.ListUtils;

/**
 * Handles the conversion of String members and String types with the {@code IdRef} trait to
 * the {@link ShapeId} type.
 *
 * <p>This integration is run with a high priority to ensure down-stream integrations see a
 * {@code ShapeId} type instead of a string.
 */
public class IdRefDecoratorIntegration implements TraitCodegenIntegration {
    private static final String INTEGRATION_NAME = "id-ref-integration";
    private static final Symbol SHAPE_ID_SYMBOL = TraitCodegenUtils.fromClass(ShapeId.class).toBuilder()
            .putProperty(SymbolProperties.TO_NODE_MAPPER, (Mapper) IdRefDecoratorIntegration::toNodeMapper)
            .putProperty(SymbolProperties.FROM_NODE_MAPPER, (Mapper) IdRefDecoratorIntegration::fromNodeMapper)
            .build();

    @Override
    public String name() {
        return INTEGRATION_NAME;
    }

    @Override
    public byte priority() {
        // Make sure this runs before all other integration
        return 127;
    }

    @Override
    public SymbolProvider decorateSymbolProvider(Model model, TraitCodegenSettings settings,
                                                 SymbolProvider symbolProvider) {
        return new SymbolProvider() {
            @Override
            public Symbol toSymbol(Shape shape) {
                return provideSymbol(shape, symbolProvider, model);
            }

            // Necessary to ensure initial toMemberName is not squashed by decorating
            @Override
            public String toMemberName(MemberShape shape) {
                return symbolProvider.toMemberName(shape);
            }
        };
    }

    private Symbol provideSymbol(Shape shape, SymbolProvider symbolProvider, Model model) {
        if (shape.hasTrait(IdRefTrait.class)) {
            return SHAPE_ID_SYMBOL;
        } else if (shape.isMemberShape()) {
            Shape target = model.expectShape(shape.asMemberShape().orElseThrow(RuntimeException::new).getTarget());
            return provideSymbol(target, symbolProvider, model);
        } else if (shape.isListShape()) {
            // Replace any members reference by a list shape as the decorator does wrap the internal call from the
            // toSymbol(member)
            MemberShape member = shape.asListShape().orElseThrow(RuntimeException::new).getMember();
            return symbolProvider.toSymbol(shape).toBuilder()
                    .references(ListUtils.of(new SymbolReference(provideSymbol(member, symbolProvider, model))))
                    .build();
        } else if (shape.isMapShape()) {
            // Same as list replacement but for map shapes
            MapShape mapShape = shape.asMapShape().orElseThrow(RuntimeException::new);
            return symbolProvider.toSymbol(shape)
                    .toBuilder()
                    .references(ListUtils.of(
                            new SymbolReference(provideSymbol(mapShape.getKey(), symbolProvider, model)),
                            new SymbolReference(provideSymbol(mapShape.getValue(), symbolProvider, model))
                    ))
                    .build();
        }
        return symbolProvider.toSymbol(shape);
    }

    private static void toNodeMapper(TraitCodegenWriter writer, String var) {
        writer.write("$T.from($L.toString())", Node.class, var);
    }

    private static void fromNodeMapper(TraitCodegenWriter writer, String var) {
        writer.write("$T.fromNode($L)", ShapeId.class, var);
    }
}
