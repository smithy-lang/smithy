/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.generators;

import java.util.Objects;
import java.util.function.Consumer;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.traitcodegen.GenerateTraitDirective;
import software.amazon.smithy.traitcodegen.sections.ClassSection;
import software.amazon.smithy.traitcodegen.sections.EnumVariantSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

/**
 * Effectively sealed base class for generating a Java Enum class from a Smithy model.
 *
 * <p>The two public implementations provided by this base class are:
 * <dl>
 *     <dt>{@link StringEnumShapeGenerator}</dt>
 *     <dd>Generates a java enum from a Smithy {@link software.amazon.smithy.model.shapes.EnumShape}.</dd>
 *     <dt>{@link IntEnumShapeGenerator}</dt>
 *     <dd>Generates a java enum from a Smithy {@link software.amazon.smithy.model.shapes.IntEnumShape}.</dd>
 * </dl>
 */
abstract class EnumShapeGenerator implements Consumer<GenerateTraitDirective> {

    // Private constructor to make abstract class effectively sealed.
    private EnumShapeGenerator() {}

    @Override
    public void accept(GenerateTraitDirective directive) {
        directive.context()
                .writerDelegator()
                .useShapeWriter(directive.shape(),
                        writer -> writeEnum(directive.shape(), directive.symbolProvider(), writer, directive.model()));
    }

    public void writeEnum(
            Shape enumShape,
            SymbolProvider provider,
            TraitCodegenWriter writer,
            Model model
    ) {
        writeEnum(enumShape, provider, writer, model, true);
    }

    /**
     * Writes an Enum class from an enum shape.
     *
     * @param enumShape enum shape to generate enum class for.
     * @param provider symbol provider.
     * @param writer writer to write generated code to.
     * @param model smithy model used for code generation.
     * @param isStandaloneClass flag indicating if enum is a standalone class (i.e. defined in its own class file).
     */
    public void writeEnum(
            Shape enumShape,
            SymbolProvider provider,
            TraitCodegenWriter writer,
            Model model,
            boolean isStandaloneClass
    ) {
        Symbol enumSymbol = provider.toSymbol(enumShape);
        writer.pushState(new ClassSection(enumShape))
                .putContext("standalone", isStandaloneClass)
                .openBlock("public enum $B ${?standalone}implements $T ${/standalone}{",
                        "}",
                        enumSymbol,
                        ToNode.class,
                        () -> {
                            writeVariants(enumShape, provider, writer);
                            writer.newLine();

                            writeValueField(writer);
                            writer.newLine();

                            writeConstructor(enumSymbol, writer);

                            writeValueGetter(writer);
                            writer.newLine();

                            writeFromMethod(enumSymbol, writer);

                            // Only generate From and To Node when we are in a standalone class.
                            if (isStandaloneClass) {
                                writeToNode(writer);
                                new FromNodeGenerator(writer, enumSymbol, enumShape, provider, model).run();
                            }
                        })
                .popState();
    }

    abstract String getVariantTemplate();

    abstract Class<?> getValueType();

    abstract Object getEnumValue(MemberShape member);

    private void writeVariants(Shape enumShape, SymbolProvider provider, TraitCodegenWriter writer) {
        for (MemberShape member : enumShape.members()) {
            writer.pushState(new EnumVariantSection(member));
            writer.write(getVariantTemplate() + ",", provider.toMemberName(member), getEnumValue(member));
            writer.popState();
        }
        writer.write("UNKNOWN(null);");
    }

    private void writeValueField(TraitCodegenWriter writer) {
        writer.write("private final $T value;", getValueType());
    }

    private void writeValueGetter(TraitCodegenWriter writer) {
        writer.openBlock("public $T getValue() {",
                "}",
                getValueType(),
                () -> writer.writeWithNoFormatting("return value;"));
    }

    private void writeToNode(TraitCodegenWriter writer) {
        writer.override();
        writer.openBlock("public $T toNode() {",
                "}",
                Node.class,
                () -> writer.write("return $T.from(value);", Node.class));
        writer.newLine();
    }

    private void writeConstructor(Symbol enumSymbol, TraitCodegenWriter writer) {
        writer.openBlock("$B($T value) {",
                "}",
                enumSymbol,
                getValueType(),
                () -> writer.write("this.value = value;"));
        writer.newLine();
    }

    private void writeFromMethod(Symbol enumSymbol, TraitCodegenWriter writer) {
        writer.writeDocString(writer.format("Create a {@code $1B} from a value in a model.\n\n"
                + "<p>Any unknown value is returned as {@code UNKNOWN}.\n"
                + "@param value Value to create enum from.\n"
                + "@return Returns the {@link $1B} enum value.", enumSymbol));
        writer.openBlock("public static $B from($T value) {",
                "}",
                enumSymbol,
                getValueType(),
                () -> {
                    writer.write("$T.requireNonNull(value, \"Enum value should not be null.\");", Objects.class);
                    writer.openBlock("for ($B val: values()) {",
                            "}",
                            enumSymbol,
                            () -> writer.openBlock("if ($T.equals(val.getValue(), value)) {",
                                    "}",
                                    Objects.class,
                                    () -> writer.writeWithNoFormatting("return val;")));
                    writer.writeWithNoFormatting("return UNKNOWN;");
                });
        writer.newLine();
    }

    /**
     * Generates a Java Enum class from a smithy {@link software.amazon.smithy.model.shapes.EnumShape}.
     */
    public static final class StringEnumShapeGenerator extends EnumShapeGenerator {
        @Override
        String getVariantTemplate() {
            return "$L($S)";
        }

        @Override
        Class<?> getValueType() {
            return String.class;
        }

        @Override
        Object getEnumValue(MemberShape member) {
            return member.expectTrait(EnumValueTrait.class).expectStringValue();
        }
    }

    /**
     * Generates a Java Enum class from a smithy {@link software.amazon.smithy.model.shapes.IntEnumShape}.
     */
    public static final class IntEnumShapeGenerator extends EnumShapeGenerator {
        @Override
        String getVariantTemplate() {
            return "$L($L)";
        }

        @Override
        Class<?> getValueType() {
            return Integer.class;
        }

        @Override
        Object getEnumValue(MemberShape member) {
            return member.expectTrait(EnumValueTrait.class).expectIntValue();
        }
    }
}
