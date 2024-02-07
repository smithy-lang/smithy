/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.traitcodegen.GenerateTraitDirective;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

/**
 * Generates a "Value trait".
 *
 * <p>A "value trait" is a trait that has only one property "value" such as a string trait or
 * a number trait. The value held by the trait is stored in a single property {@code value} and
 * will have a {@code getValue()} getter to allow access to the held value.
 */
final class ValueTraitGenerator extends TraitGenerator {
    @Override
    protected void writeTraitBody(TraitCodegenWriter writer, GenerateTraitDirective directive) {
        new PropertiesGenerator(writer, directive.shape(), directive.symbolProvider()).run();
        writeConstructor(writer, directive.symbol());
        writeConstructorWithSourceLocation(writer, directive.symbol());
        new ToNodeGenerator(writer, directive.shape(), directive.symbolProvider(), directive.model()).run();
        new GetterGenerator(writer, directive.symbolProvider(), directive.shape(), directive.model()).run();
    }

    private void writeConstructorWithSourceLocation(TraitCodegenWriter writer, Symbol traitSymbol) {
        writer.addImport(FromSourceLocation.class);
        writer.openBlock("public $1T($1B value, FromSourceLocation sourceLocation) {", "}",
                traitSymbol, () -> {
                    writer.write("super(ID, sourceLocation);");
                    writer.write("this.value = value;");
                });
        writer.newLine();
    }

    private void writeConstructor(TraitCodegenWriter writer, Symbol traitSymbol) {
        writer.addImport(SourceLocation.class);
        writer.openBlock("public $1T($1B value) {", "}", traitSymbol, () -> {
            writer.write("super(ID, SourceLocation.NONE);");
            writer.write("this.value = value;");
        });
        writer.newLine();
    }
}
