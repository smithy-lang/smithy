/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators.base;

import java.util.Iterator;
import java.util.function.Consumer;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.traitcodegen.generators.common.FromNodeGenerator;
import software.amazon.smithy.traitcodegen.sections.ClassSection;
import software.amazon.smithy.traitcodegen.sections.EnumVariantSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

abstract class AbstractEnumGenerator<T> implements Consumer<T> {
    private static final String VALUE_FIELD_TEMPLATE = "private final $T value;";

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

    abstract Symbol getValueType();

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
                () -> writer.write("return value;"));
    }

    private void writeConstructor(Symbol enumSymbol, TraitCodegenWriter writer) {
        writer.openBlock("$T($T value) {", "}",
                enumSymbol, getValueType(), () -> writer.write("this.value = value;"));
        writer.newLine();
    }
}
