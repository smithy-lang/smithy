/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.DocumentShape;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.StringTrait;
import software.amazon.smithy.model.traits.Trait;
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
    private final Symbol traitSymbol;
    private final Model model;
    private final Shape shape;

    ProviderGenerator(TraitCodegenWriter writer, Shape shape, Symbol traitSymbol, Model model) {
        this.writer = writer;
        this.traitSymbol = traitSymbol;
        this.model = model;
        this.shape = shape;
    }

    @Override
    public void run() {
        shape.accept(new ProviderMethodVisitor());
    }

    private final class ProviderMethodVisitor extends ShapeVisitor.Default<Void> {
        @Override
        public Void getDefault(Shape shape) {
            writer.openBlock("public static final class Provider extends $T.Provider {", "}",
                    AbstractTrait.class, () -> {
                // Basic constructor
                generateProviderConstructor();

                // Provider method
                writer.override();
                writer.openBlock("public $T createTrait($T target, $T value) {", "}",
                        Trait.class, ShapeId.class, Node.class,
                        () -> writer.write("return new $T($C, value.getSourceLocation());",
                                traitSymbol,
                                (Runnable) () -> shape.accept(
                                        new FromNodeMapperVisitor(writer, model, "value"))));
            });
            return null;
        }

        @Override
        public Void documentShape(DocumentShape shape) {
            writer.openBlock("public static final class Provider extends $T.Provider {", "}",
                    AbstractTrait.class, () -> {
                generateProviderConstructor();
                writer.newLine();
                writer.override();
                writer.openBlock("public $T createTrait($T target, $T value) {", "}",
                        Trait.class, ShapeId.class, Node.class,
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
                    StringTrait.class, traitSymbol, () -> writer.openBlock("public Provider() {", "}",
                            () -> writer.write("super(ID, $T::new);", traitSymbol)));
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            generateAbstractTraitProvider();
            return null;
        }

        private void generateAbstractTraitProvider() {
            writer.openBlock("public static final class Provider extends $T.Provider {", "}",
                    AbstractTrait.class, () -> {
                generateProviderConstructor();
                writer.override();
                writer.openBlock("public $T createTrait($T target, $T value) {", "}",
                        Trait.class, ShapeId.class, Node.class, () -> {
                    writer.write("$1T result = $1T.fromNode(value);", traitSymbol);
                    writer.writeWithNoFormatting("result.setNodeCache(value);");
                    writer.writeWithNoFormatting("return result;");
                });
            });
        }

        private void generateProviderConstructor() {
            writer.openBlock("public Provider() {", "}", () -> writer.write("super(ID);")).newLine();
        }
    }
}
