/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.traits.StringListTrait;
import software.amazon.smithy.traitcodegen.GenerateTraitDirective;
import software.amazon.smithy.traitcodegen.sections.BuilderClassSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

/**
 * Generates traits for the special case where the list's member is represented by a
 * java string symbol.
 *
 * <p>List Traits with only a member shape that can be represented as a java string
 * can use the base trait class {@link StringListTrait}.
 */
final class StringListTraitGenerator extends TraitGenerator {
    @Override
    protected void writeProvider(TraitCodegenWriter writer, GenerateTraitDirective directive) {
        writer.openBlock("public static final class Provider extends $T.Provider<$T> {", "}",
                StringListTrait.class, directive.symbol(),
                () -> writer.openBlock("public Provider() {", "}",
                        () -> writer.write("super(ID, $T::new);", directive.symbol())));
    }

    @Override
    protected Class<?> getBaseClass() {
        return StringListTrait.class;
    }

    @Override
    protected boolean implementsToSmithyBuilder() {
        return true;
    }

    @Override
    protected void writeTraitBody(TraitCodegenWriter writer, GenerateTraitDirective directive) {
        writeConstructorWithSourceLocation(writer, directive.symbol());
        writeConstructor(writer, directive.symbol());
        writeToBuilderMethod(writer, directive.symbol());
        writeBuilderGetter(writer);
        writeBuilderClass(writer, directive.symbol());
    }

    private void writeToBuilderMethod(TraitCodegenWriter writer, Symbol symbol) {
        writer.openDocstring();
        writer.writeDocStringContents("Creates a builder used to build a {@link $T}.", symbol);
        writer.closeDocstring();
        writer.override();
        writer.openBlock("public Builder toBuilder() {", "}",
                () -> writer.write("return builder().sourceLocation(getSourceLocation()).values(getValues());"));
        writer.newLine();
    }

    private void writeBuilderGetter(TraitCodegenWriter writer) {
        writer.openBlock("public static Builder builder() {", "}",
                () -> writer.writeWithNoFormatting("return new Builder();"));
        writer.newLine();
    }

    private void writeBuilderClass(TraitCodegenWriter writer, Symbol symbol) {
        writer.pushState(new BuilderClassSection(symbol));
        writer.openBlock("public static final class Builder extends $T.Builder<$T, Builder> {", "}",
                StringListTrait.class, symbol, () -> {
                    writer.writeWithNoFormatting("private Builder() {}");
                    writer.newLine();
                    writer.override();
                    writer.openBlock("public $T build() {", "}", symbol,
                            () -> writer.write("return new $T(getValues(), getSourceLocation());", symbol));
                });
        writer.popState();
        writer.newLine();
    }


    private void writeConstructorWithSourceLocation(TraitCodegenWriter writer, Symbol traitSymbol) {
        writer.openBlock("public $1T($1B values, $2T sourceLocation) {", "}",
                traitSymbol, FromSourceLocation.class, () -> writer.write("super(ID, values, sourceLocation);"));
        writer.newLine();
    }

    private void writeConstructor(TraitCodegenWriter writer, Symbol traitSymbol) {
        writer.openBlock("public $1T($1B values) {", "}", traitSymbol,
                () -> writer.write("super(ID, values, $T.NONE);", SourceLocation.class));
        writer.newLine();
    }
}
