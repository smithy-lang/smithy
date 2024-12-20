/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.generators;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.DocumentShape;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.NumberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.StringListTrait;
import software.amazon.smithy.model.traits.StringTrait;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

/**
 * Adds provider class to use as the {@link software.amazon.smithy.model.traits.TraitService} implementation for a
 * trait.
 *
 * <p>This provider class is only required for Trait classes, and is not needed in nested shapes. The {@code fromNode}
 * method is used where possible to create the smithy node from the provided node. Provider methods MUST
 * be added to the {@code META-INF/services/software.amazon.smithy.model.traits.TraitService} service provider file
 * for the Trait class to be discovered during model assembly.
 */
final class ProviderGenerator implements Runnable {

    private final TraitCodegenWriter writer;
    private final Model model;
    private final Shape shape;
    private final SymbolProvider provider;
    private final Symbol traitSymbol;

    ProviderGenerator(
            TraitCodegenWriter writer,
            Model model,
            Shape shape,
            SymbolProvider provider,
            Symbol traitSymbol
    ) {
        this.writer = writer;
        this.model = model;
        this.shape = shape;
        this.provider = provider;
        this.traitSymbol = traitSymbol;
    }

    @Override
    public void run() {
        shape.accept(new ProviderMethodVisitor());
    }

    private final class ProviderMethodVisitor extends TraitVisitor<Void> {

        @Override
        public Void documentShape(DocumentShape shape) {
            writer.openBlock("public static final class Provider extends $T.Provider {",
                    "}",
                    AbstractTrait.class,
                    () -> {
                        generateProviderConstructor();
                        writer.newLine();
                        writer.override();
                        writer.openBlock("public $T createTrait($T target, $T value) {",
                                "}",
                                Trait.class,
                                ShapeId.class,
                                Node.class,
                                () -> writer.write("return new $T(value);", traitSymbol));
                    });
            return null;
        }

        @Override
        public Void stringShape(StringShape shape) {
            // If the symbol resolves to a simple java string use the simplified string
            // provider. Otherwise, use a generic value shape provider.
            if (TraitCodegenUtils.isJavaString(traitSymbol)) {
                generateStringShapeProvider();
            } else {
                generateValueShapeProvider();
            }
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            generateAbstractTraitProvider();
            return null;
        }

        @Override
        public Void listShape(ListShape shape) {
            // If the trait is a string-only list we can use a simpler provider from the StringListTrait base class
            if (TraitCodegenUtils.isJavaStringList(shape, provider)) {
                writer.openBlock("public static final class Provider extends $T.Provider<$T> {",
                        "}",
                        StringListTrait.class,
                        traitSymbol,
                        () -> writer.openBlock("public Provider() {",
                                "}",
                                () -> writer.write("super(ID, $T::new);", traitSymbol)));
            } else {
                generateAbstractTraitProvider();
            }
            return null;
        }

        @Override
        public Void enumShape(EnumShape shape) {
            generateStringShapeProvider();
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            generateAbstractTraitProvider();
            return null;
        }

        @Override
        public Void timestampShape(TimestampShape shape) {
            generateAbstractTraitProvider();
            return null;
        }

        @Override
        protected Void numberShape(NumberShape shape) {
            generateValueShapeProvider();
            return null;
        }

        private void generateAbstractTraitProvider() {
            writer.openBlock("public static final class Provider extends $T.Provider {",
                    "}",
                    AbstractTrait.class,
                    () -> {
                        generateProviderConstructor();
                        writer.override();
                        writer.openBlock("public $T createTrait($T target, $T value) {",
                                "}",
                                Trait.class,
                                ShapeId.class,
                                Node.class,
                                () -> {
                                    writer.write("$1T result = $1T.fromNode(value);", traitSymbol);
                                    writer.writeWithNoFormatting("result.setNodeCache(value);");
                                    writer.writeWithNoFormatting("return result;");
                                });
                    });
        }

        private void generateProviderConstructor() {
            writer.openBlock("public Provider() {", "}", () -> writer.write("super(ID);")).newLine();
        }

        private void generateValueShapeProvider() {
            writer.openBlock("public static final class Provider extends $T.Provider {",
                    "}",
                    AbstractTrait.class,
                    () -> {
                        generateProviderConstructor();
                        writer.override();
                        writer.openBlock("public $T createTrait($T target, $T value) {",
                                "}",
                                Trait.class,
                                ShapeId.class,
                                Node.class,
                                () -> writer.write("return new $1T($2C, value.getSourceLocation());",
                                        traitSymbol,
                                        (Runnable) () -> shape
                                                .accept(new FromNodeMapperVisitor(writer, model, "value"))));
                    });
        }

        private void generateStringShapeProvider() {
            writer.openBlock("public static final class Provider extends $T.Provider<$T> {",
                    "}",
                    StringTrait.class,
                    traitSymbol,
                    () -> writer.openBlock("public Provider() {",
                            "}",
                            () -> writer.write("super(ID, $T::new);", traitSymbol)));
        }
    }
}
