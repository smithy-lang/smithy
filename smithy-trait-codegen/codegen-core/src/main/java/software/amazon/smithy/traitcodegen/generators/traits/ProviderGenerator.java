/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators.traits;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.DocumentShape;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.StringTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.traitcodegen.SymbolProperties;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;


/**
 * Adds provider class to use as the {@link software.amazon.smithy.model.traits.TraitService} implementation for a
 * trait.
 */
final class ProviderGenerator implements Runnable {
    private static final String PROVIDER_METHOD = "public Provider() {";

    private final TraitCodegenWriter writer;
    private final Symbol traitSymbol;
    private final Shape shape;
    private final SymbolProvider symbolProvider;

    ProviderGenerator(TraitCodegenWriter writer, Shape shape, Symbol traitSymbol, SymbolProvider symbolProvider) {
        this.writer = writer;
        this.traitSymbol = traitSymbol;
        this.shape = shape;
        this.symbolProvider = symbolProvider;
    }

    @Override
    public void run() {
        shape.accept(new ProviderMethodVisitor());
    }

    private final class ProviderMethodVisitor extends ShapeVisitor.Default<Void> {
        @Override
        public Void getDefault(Shape shape) {
            writer.openBlock("public static final class Provider extends AbstractTrait.Provider {", "}", () -> {
                // Basic constructor
                generateProviderConstructor();

                // Provider method
                writer.addImports(Trait.class, ShapeId.class, Node.class);
                writer.override();
                writer.openBlock("public Trait createTrait(ShapeId target, Node value) {", "}",
                        () -> writer.write("return new $T("
                                + symbolProvider.toSymbol(shape).expectProperty(SymbolProperties.FROM_NODE_MAPPER)
                                + ", value.getSourceLocation());", traitSymbol, "value"));
            });
            return null;
        }

        @Override
        public Void documentShape(DocumentShape shape) {
            writer.openBlock("public static final class Provider extends AbstractTrait.Provider {", "}", () -> {
                generateProviderConstructor();
                writer.newLine();

                writer.addImports(Trait.class, ShapeId.class, Node.class);
                writer.override();
                writer.openBlock("public Trait createTrait(ShapeId target, Node value) {", "}",
                        () -> writer.write("return new $T(value);", traitSymbol));
            });
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            generateAbstractTraitProvider();
            return null;
        }

        @Override
        public Void listShape(ListShape shape) {
            generateAbstractTraitProvider();
            return null;
        }

        @Override
        public Void enumShape(EnumShape shape) {
            writer.openBlock("public static final class Provider extends $T.Provider<$T> {", "}",
                    TraitCodegenUtils.fromClass(StringTrait.class),
                    traitSymbol, () -> writer.openBlock(PROVIDER_METHOD, "}",
                            () -> writer.write("super(ID, $T::new);", traitSymbol)));
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            generateAbstractTraitProvider();
            return null;
        }

        private void generateAbstractTraitProvider() {
            writer.addImports(Trait.class, Node.class);
            writer.openBlock("public static final class Provider extends AbstractTrait.Provider {", "}", () -> {
                generateProviderConstructor();

                writer.override();
                writer.openBlock("public Trait createTrait(ShapeId target, Node value) {", "}", () -> {
                    writer.write("$1T result = $1T.fromNode(value);", traitSymbol);
                    writer.write("result.setNodeCache(value);");
                    writer.write("return result;");
                });
            });
        }

        private void generateProviderConstructor() {
            writer.openBlock(PROVIDER_METHOD, "}", () -> writer.write("super(ID);")).newLine();
        }
    }
}
