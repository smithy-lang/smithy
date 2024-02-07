/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators;

import java.util.function.Consumer;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.traitcodegen.GenerateTraitDirective;
import software.amazon.smithy.traitcodegen.sections.ClassSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

/**
 * Generates a Java class from a Smithy {@code StructureShape}.
 */
final class StructureShapeGenerator implements Consumer<GenerateTraitDirective> {
    private static final String BASE_CLASS_TEMPLATE_STRING = "public final class $1T implements ToNode, "
            + "ToSmithyBuilder<$1T> {";

    @Override
    public void accept(GenerateTraitDirective directive) {
        directive.context().writerDelegator().useShapeWriter(directive.shape(), writer -> {
            writer.addImport(ToNode.class);
            writer.pushState(new ClassSection(directive.shape()))
                    .openBlock(BASE_CLASS_TEMPLATE_STRING, "}", directive.symbol(), () -> {
                        new PropertiesGenerator(writer, directive.shape(), directive.symbolProvider()).run();
                        new ConstructorWithBuilderGenerator(writer, directive.symbol(), directive.shape(),
                                directive.symbolProvider()).run();
                        new ToNodeGenerator(writer, directive.shape(), directive.symbolProvider(),
                                directive.model()).run();
                        new FromNodeGenerator(writer, directive.symbol(), directive.shape(),
                                directive.symbolProvider(), directive.model()).run();
                        new GetterGenerator(writer, directive.symbolProvider(), directive.shape(),
                                directive.model()).run();
                        new BuilderGenerator(writer, directive.symbol(), directive.symbolProvider(),
                                directive.shape(), directive.model()).run();
                        writeEquals(writer, directive.symbol());
                        writeHashCode(writer);
                    })
                    .popState();
            writer.newLine();
        });
    }

    private void writeEquals(TraitCodegenWriter writer, Symbol symbol) {
        writer.override();
        writer.openBlock("public boolean equals(Object other) {", "}", () -> {
            writer.disableNewlines();
            writer.openBlock("if (other == this) {\n", "}",
                    () -> writer.write("return true;").newLine());
            writer.openBlock(" else if (!(other instanceof $T)) {\n", "}", symbol,
                    () -> writer.write("return false;").newLine());
            writer.openBlock(" else {\n", "}", () -> {
                writer.write("$1T b = ($1T) other;", symbol).newLine();
                writer.write("return toNode().equals(b.toNode());\n");
            }).newLine();
            writer.enableNewlines();
        });
        writer.newLine();
    }

    private void writeHashCode(TraitCodegenWriter writer) {
        writer.override();
        writer.openBlock("public int hashCode() {", "}",
                () -> writer.write("return toNode().hashCode();"));
    }
}
