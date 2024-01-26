/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators.traits;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.traits.StringListTrait;
import software.amazon.smithy.traitcodegen.GenerateTraitDirective;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
import software.amazon.smithy.traitcodegen.sections.BuilderClassSection;
import software.amazon.smithy.traitcodegen.sections.ToBuilderSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

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
        writer.addImport(StringListTrait.class);
        writer.openBlock("public static final class Provider extends StringListTrait.Provider<$T> {", "}",
                directive.symbol(), () -> writer.openBlock("public Provider() {", "}",
                        () -> writer.write("super(ID, $T::new);", directive.symbol())));
    }

    @Override
    protected Symbol getBaseClass() {
        return TraitCodegenUtils.fromClass(StringListTrait.class);
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
        writer.pushState(new ToBuilderSection(symbol));
        writer.addImports(SmithyBuilder.class, ToSmithyBuilder.class);
        writer.override();
        writer.openBlock("public Builder toBuilder() {", "}",
                () -> writer.write("return builder().sourceLocation(getSourceLocation()).values(getValues());"));
        writer.popState();
        writer.newLine();
    }

    private void writeBuilderGetter(TraitCodegenWriter writer) {
        writer.openBlock("public static Builder builder() {", "}",
                () -> writer.write("return new Builder();"));
        writer.newLine();
    }

    private void writeBuilderClass(TraitCodegenWriter writer, Symbol symbol) {
        writer.pushState(new BuilderClassSection(symbol));
        writer.addImport(StringListTrait.class);
        writer.openBlock("public static final class Builder extends StringListTrait.Builder<$T, Builder> {", "}",
                symbol, () -> {
                    writer.write("private Builder() {}");
                    writer.newLine();
                    writer.override();
                    writer.openBlock("public $T build() {", "}", symbol,
                            () -> writer.write("return new $T(getValues(), getSourceLocation());", symbol));
                });
        writer.popState();
        writer.newLine();
    }


    private void writeConstructorWithSourceLocation(TraitCodegenWriter writer, Symbol traitSymbol) {
        writer.addImport(FromSourceLocation.class);
        writer.openBlock("public $1T($1B values, FromSourceLocation sourceLocation) {", "}",
                traitSymbol, () -> writer.write("super(ID, values, sourceLocation);"));
        writer.newLine();
    }

    private void writeConstructor(TraitCodegenWriter writer, Symbol traitSymbol) {
        writer.addImport(SourceLocation.class);
        writer.openBlock("public $1T($1B values) {", "}", traitSymbol,
                () -> writer.write("super(ID, values, SourceLocation.NONE);"));
        writer.newLine();
    }
}
