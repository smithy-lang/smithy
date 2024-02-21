/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators;

import java.util.Iterator;
import java.util.function.Consumer;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
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
 * <ul>
 *     <li>{@link StringEnumShapeGenerator} - Generates a java enum from a Smithy
 *     {@link software.amazon.smithy.model.shapes.EnumShape}.</li>
 *     <li>{@link IntEnumShapeGenerator} - Generates a java enum from a Smithy
 *     {@link software.amazon.smithy.model.shapes.IntEnumShape}.</li>
 * </ul>
 *
 */
abstract class EnumShapeGenerator implements Consumer<GenerateTraitDirective> {
    private static final String VALUE_FIELD_TEMPLATE = "private final $T value;";

    // Private constructor to make abstract class effectively sealed.
    private EnumShapeGenerator() {}

    @Override
    public void accept(GenerateTraitDirective directive) {
        directive.context().writerDelegator().useShapeWriter(directive.shape(),
                writer -> writeEnum(directive.shape(), directive.symbolProvider(), writer, directive.model()));
    }

    protected void writeEnum(Shape enumShape, SymbolProvider provider, TraitCodegenWriter writer, Model model) {
        Symbol enumSymbol = provider.toSymbol(enumShape);
        writer.pushState(new ClassSection(enumShape))
                .openBlock("public enum $L {", "}", enumSymbol.getName(), () -> {
                    writeVariants(enumShape, provider, writer);
                    writer.newLine();

                    writeValueField(writer);
                    writer.newLine();

                    writeConstructor(enumSymbol, writer);

                    writeValueGetter(writer);
                    writer.newLine();

                    new FromNodeGenerator(writer, enumSymbol, enumShape, provider, model).run();
                })
                .popState();
    }

    abstract String getVariantTemplate();

    abstract Class<?> getValueType();

    abstract Object getEnumValue(MemberShape member);

    private void writeVariants(Shape enumShape, SymbolProvider provider, TraitCodegenWriter writer) {
        Iterator<MemberShape> memberIterator = enumShape.members().iterator();
        String template = getVariantTemplate();
        while (memberIterator.hasNext()) {
            MemberShape member = memberIterator.next();
            String name = provider.toMemberName(member);
            writer.pushState(new EnumVariantSection(member));
            if (memberIterator.hasNext()) {
                writer.write(template + ",", name, getEnumValue(member));
            } else {
                writer.write(template + ";", name, getEnumValue(member));
            }
            writer.popState();
        }
    }

    private void writeValueField(TraitCodegenWriter writer) {
        writer.write(VALUE_FIELD_TEMPLATE, getValueType());
    }

    private void writeValueGetter(TraitCodegenWriter writer) {
        writer.openBlock("public $T getValue() {", "}", getValueType(),
                () -> writer.writeWithNoFormatting("return value;"));
    }

    private void writeConstructor(Symbol enumSymbol, TraitCodegenWriter writer) {
        writer.openBlock("$T($T value) {", "}",
                enumSymbol, getValueType(), () -> writer.write("this.value = value;"));
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
            return int.class;
        }

        @Override
        Object getEnumValue(MemberShape member) {
            return member.expectTrait(EnumValueTrait.class).expectIntValue();
        }
    }
}
