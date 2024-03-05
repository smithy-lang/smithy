/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.StringTrait;
import software.amazon.smithy.traitcodegen.GenerateTraitDirective;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

/**
 * Generates a Java class representation of a Smithy {@link EnumShape} trait.
 *
 * <p>Note: This generator only generates Java classes for string enums. For generating
 * a trait class from an {@link software.amazon.smithy.model.shapes.IntEnumShape}, use
 * the {@link IntEnumTraitGenerator} generator.
 */
final class EnumTraitGenerator extends TraitGenerator {

    @Override
    protected Class<?> getBaseClass() {
        return StringTrait.class;
    }

    @Override
    protected void writeTraitBody(TraitCodegenWriter writer, GenerateTraitDirective directive) {
        writeConstructor(writer, directive.symbol());
        writeConstructorWithSourceLocation(writer, directive.symbol());
        new GetterGenerator(writer, directive.symbolProvider(), directive.model(), directive.shape()).run();
        writeNestedEnumClass(writer, directive.shape(), directive.symbolProvider(), directive.model());
    }

    private void writeConstructorWithSourceLocation(TraitCodegenWriter writer, Symbol symbol) {
        writer.openBlock("public $T($T value, $T sourceLocation) {", "}",
                symbol, String.class, FromSourceLocation.class,
                () -> writer.writeWithNoFormatting("super(ID, value, sourceLocation);"));
        writer.newLine();
    }

    private void writeConstructor(TraitCodegenWriter writer, Symbol symbol) {
        writer.openBlock("public $T($T value) {", "}", symbol, String.class,
                () -> writer.write("super(ID, value, $T.NONE);", SourceLocation.class));
        writer.newLine();
    }

    private void writeNestedEnumClass(TraitCodegenWriter writer,
                                        Shape shape,
                                        SymbolProvider symbolProvider,
                                        Model model
    ) {
        new EnumShapeGenerator.StringEnumShapeGenerator().writeEnum(shape, symbolProvider, writer, model, false);
        writer.newLine();
    }
}
