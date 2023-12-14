/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.integrations.strings;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.StringListTrait;
import software.amazon.smithy.traitcodegen.GenerateTraitDirective;
import software.amazon.smithy.traitcodegen.generators.traits.TraitGenerator;
import software.amazon.smithy.traitcodegen.sections.BuilderClassSection;
import software.amazon.smithy.traitcodegen.sections.ToBuilderSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

final class StringListTraitGenerator extends TraitGenerator {
    private static final String CLASS_TEMPLATE = "public final class $1T extends StringListTrait implements "
            + "ToSmithyBuilder<$1T> {";

    @Override
    protected void writeProvider(TraitCodegenWriter writer, GenerateTraitDirective directive) {
        writer.addImport(StringListTrait.class);
        writer.openBlock("public static final class Provider extends StringListTrait.Provider<$T> {", "}",
                directive.traitSymbol(), () -> writer.openBlock("public Provider() {", "}",
                        () -> writer.write("super(ID, $T::new);", directive.traitSymbol())));
    }

    @Override
    protected void imports(TraitCodegenWriter writer) {
        writer.addImport(StringListTrait.class);
    }

    @Override
    protected String getClassDefinition() {
        return CLASS_TEMPLATE;
    }

    @Override
    protected void writeTraitBody(TraitCodegenWriter writer, GenerateTraitDirective directive) {
        writeConstructorWithSourceLocation(writer, directive.traitSymbol(), directive.baseSymbol());
        writeConstructor(writer, directive.traitSymbol(), directive.baseSymbol());
        writeToBuilderMethod(writer, directive.shape(), directive.traitSymbol());
        writeBuilderGetter(writer);
        writeBuilderClass(writer, directive.shape(), directive.traitSymbol());
    }

    private void writeToBuilderMethod(TraitCodegenWriter writer, Shape shape, Symbol symbol) {
        writer.pushState(new ToBuilderSection(shape, symbol));
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

    private void writeBuilderClass(TraitCodegenWriter writer, Shape shape, Symbol symbol) {
        writer.pushState(new BuilderClassSection(shape, symbol));
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


    private void writeConstructorWithSourceLocation(TraitCodegenWriter writer, Symbol traitSymbol, Symbol baseSymbol) {
        writer.addImport(FromSourceLocation.class);
        writer.openBlock("public $T($T values, FromSourceLocation sourceLocation) {", "}",
                traitSymbol, baseSymbol,
                () -> writer.write("super(ID, values, sourceLocation);"));
        writer.newLine();
    }

    private void writeConstructor(TraitCodegenWriter writer, Symbol traitSymbol, Symbol baseSymbol) {
        writer.addImport(SourceLocation.class);
        writer.openBlock("public $T($T values) {", "}", traitSymbol, baseSymbol,
                () -> writer.write("super(ID, values, SourceLocation.NONE);"));
        writer.newLine();
    }
}
