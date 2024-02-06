/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators;

import java.util.Arrays;
import java.util.stream.Collectors;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.traits.StringTrait;
import software.amazon.smithy.traitcodegen.GenerateTraitDirective;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.StringUtils;

/**
 * Generates a Java class representation of a Smithy {@link EnumShape} trait.
 *
 * <p>Note: This generator only generates Java classes for string enums. For generating
 * a trait class from an {@link software.amazon.smithy.model.shapes.IntEnumShape}, use
 * the {@link IntEnumTraitGenerator} generator.
 */
final class EnumTraitGenerator extends TraitGenerator {
    @Override
    protected Symbol getBaseClass() {
        return TraitCodegenUtils.fromClass(StringTrait.class);
    }

    @Override
    protected void writeTraitBody(TraitCodegenWriter writer, GenerateTraitDirective directive) {
        new PropertiesGenerator(writer, directive.shape(), directive.symbolProvider()).run();
        writeConstructor(writer, directive.symbol());
        writeConstructorWithSourceLocation(writer, directive.symbol());
        EnumShape shape = directive.shape().asEnumShape().orElseThrow(RuntimeException::new);
        for (String memberKey : shape.getEnumValues().keySet()) {
            writer.openBlock("public boolean is$L() {", "}", getMethodName(memberKey),
                            () -> writer.write("return $L.equals(getValue());", memberKey))
                    .writeInline("\n");
        }
    }

    private void writeConstructorWithSourceLocation(TraitCodegenWriter writer, Symbol symbol) {
        writer.addImport(FromSourceLocation.class);
        writer.openBlock("public $T(String name, FromSourceLocation sourceLocation) {", "}", symbol,
                () -> writer.write("super(ID, name, sourceLocation);"));
        writer.newLine();
    }

    private void writeConstructor(TraitCodegenWriter writer, Symbol symbol) {
        writer.addImport(SourceLocation.class);
        writer.openBlock("public $T(String name) {", "}", symbol,
                () -> writer.write("super(ID, name, SourceLocation.NONE);"));
        writer.newLine();
    }

    private String getMethodName(String enumValue) {
        return Arrays.stream(enumValue.split("_"))
                .map(String::toLowerCase)
                .map(StringUtils::capitalize)
                .collect(Collectors.joining());
    }
}
