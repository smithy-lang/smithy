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
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.traitcodegen.GenerateTraitDirective;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

/**
 * Generates a Java class representation of a Smithy {@code IntEnum} trait.
 *
 * <p>Note: This generator only generates Java classes for int enums. For generating
 * a trait class from an {@link software.amazon.smithy.model.shapes.EnumShape}, use
 * the {@link EnumTraitGenerator} generator.
 */
final class IntEnumTraitGenerator extends TraitGenerator {
    @Override
    protected void writeTraitBody(TraitCodegenWriter writer, GenerateTraitDirective directive) {
        new PropertiesGenerator(writer, directive.shape(), directive.symbolProvider()).run();
        writeConstructor(writer, directive.symbol());
        writeConstructorWithSourceLocation(writer, directive.symbol());
        new ToNodeGenerator(writer, directive.shape(), directive.symbolProvider(), directive.model()).run();
        new GetterGenerator(writer, directive.symbolProvider(), directive.shape()).run();
        writeNestedEnumClass(writer, directive.shape(), directive.symbolProvider(), directive.model());
    }

    private void writeConstructorWithSourceLocation(TraitCodegenWriter writer, Symbol symbol) {
        writer.openBlock("public $T($T value, $T sourceLocation) {", "}",
                symbol, Integer.class, FromSourceLocation.class, () -> {
                    writer.writeWithNoFormatting("super(ID, sourceLocation);");
                    writer.writeWithNoFormatting("this.value = value;");
                });
        writer.newLine();
    }

    private void writeConstructor(TraitCodegenWriter writer, Symbol symbol) {
        writer.openBlock("public $T($T value) {", "}",
                symbol, Integer.class, () -> {
                    writer.write("super(ID, $T.NONE);", SourceLocation.class);
                    writer.writeWithNoFormatting("this.value = value;");
                });
        writer.newLine();
    }

    private void writeNestedEnumClass(TraitCodegenWriter writer,
                                      Shape shape,
                                      SymbolProvider symbolProvider,
                                      Model model
    ) {
        new EnumShapeGenerator.IntEnumShapeGenerator().writeEnum(shape, symbolProvider, writer, model, false);
        writer.newLine();
    }
}
