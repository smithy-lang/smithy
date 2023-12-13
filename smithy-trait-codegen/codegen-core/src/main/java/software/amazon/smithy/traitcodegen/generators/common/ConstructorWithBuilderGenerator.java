/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators.common;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.traitcodegen.SymbolProperties;
import software.amazon.smithy.traitcodegen.utils.ShapeUtils;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.SmithyBuilder;

public final class ConstructorWithBuilderGenerator implements Runnable {
    private static final String CONSTRUCTOR_TEMPLATE = "private $T(Builder builder) {";

    private final TraitCodegenWriter writer;
    private final Symbol symbol;
    private final Shape shape;
    private final SymbolProvider symbolProvider;
    private final Model model;

    public ConstructorWithBuilderGenerator(TraitCodegenWriter writer, Symbol symbol, Shape shape,
                                           SymbolProvider symbolProvider, Model model) {
        this.writer = writer;
        this.symbol = symbol;
        this.shape = shape;
        this.symbolProvider = symbolProvider;
        this.model = model;
    }

    @Override
    public void run() {
        writer.openBlock(CONSTRUCTOR_TEMPLATE, "}", symbol, () -> {
            if (ShapeUtils.isTrait(shape)) {
                writer.write("super(ID, builder.getSourceLocation());");
            }
            shape.accept(new InitializerVisitor());
        });
        writer.newLine();
    }

    private final class InitializerVisitor extends ShapeVisitor.Default<Void> {

        @Override
        protected Void getDefault(Shape shape) {
            throw new UnsupportedOperationException("Does not support shape of type " + shape.getType());
        }

        @Override
        public Void listShape(ListShape shape) {
            writeValuesInitializer();
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            writeValuesInitializer();
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            for (MemberShape member : shape.members()) {
                if (member.isRequired()) {
                    writer.addImport(SmithyBuilder.class);
                    writer.write("this.$1L = SmithyBuilder.requiredState($1S, $2L);",
                            ShapeUtils.toMemberNameOrValues(member, model, symbolProvider), getBuilderValue(member));
                } else {
                    writer.write("this.$L = $L;", ShapeUtils.toMemberNameOrValues(member, model, symbolProvider),
                            getBuilderValue(member));
                }
            }
            return null;
        }

        private String getBuilderValue(MemberShape member) {
            if (symbolProvider.toSymbol(member).getProperty(SymbolProperties.BUILDER_REF_INITIALIZER).isPresent()) {
                return writer.format("builder.$L.copy()", ShapeUtils.toMemberNameOrValues(member, model,
                        symbolProvider));
            } else {
                return writer.format("builder.$L", ShapeUtils.toMemberNameOrValues(member, model, symbolProvider));
            }
        }

        private void writeValuesInitializer() {
            writer.write("this.values = builder.values.copy();");
        }
    }
}
