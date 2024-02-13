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
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.traitcodegen.GenerateTraitDirective;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.StringUtils;


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
        new GetterGenerator(writer, directive.symbolProvider(), directive.shape(), directive.model()).run();
        IntEnumShape shape = directive.shape().asIntEnumShape().orElseThrow(() -> new RuntimeException("oops"));
        for (String memberKey : shape.getEnumValues().keySet()) {
            writer.openBlock("public boolean is$L() {", "}", getMethodName(memberKey),
                    () -> writer.write("return $L.equals(getValue());", memberKey));
            writer.newLine();
        }
    }

    private void writeConstructorWithSourceLocation(TraitCodegenWriter writer, Symbol symbol) {
        writer.addImport(FromSourceLocation.class);
        writer.openBlock("public $T(Integer value, FromSourceLocation sourceLocation) {", "}", symbol, () -> {
            writer.writeWithNoFormatting("super(ID, sourceLocation);");
            writer.writeWithNoFormatting("this.value = value;");
        });
        writer.newLine();
    }

    private void writeConstructor(TraitCodegenWriter writer, Symbol symbol) {
        writer.addImport(SourceLocation.class);
        writer.openBlock("public $T(Integer value) {", "}", symbol, () -> {
            writer.writeWithNoFormatting("super(ID, SourceLocation.NONE);");
            writer.writeWithNoFormatting("this.value = value;");
        });
        writer.newLine();
    }

    private String getMethodName(String enumValue) {
        return Arrays.stream(enumValue.split("_"))
                .map(String::toLowerCase)
                .map(StringUtils::capitalize)
                .collect(Collectors.joining());
    }
}
