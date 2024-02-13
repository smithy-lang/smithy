/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators;

import java.util.Map;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.DocumentShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.traits.EnumValueTrait;
import software.amazon.smithy.traitcodegen.sections.EnumVariantSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

/**
 * Generates properties for a Java class from Smithy shape members.
 *
 * <p>The generated properties hold the value types of member shapes or a value property representing
 * the data the trait holds. In the following two cases the generated property has a static
 * name:
 * <ul>
 *     <li>Value Shapes (numbers, enum, strings) - property {@code "value"} represents the single data type
 *     held by the trait such as a {@code int} value.</li>
 *     <li>List and Map shapes - property {@code "values"} represents the collection held by the trait such
 *     as a list of strings.</li>
 * </ul>
 *
 */
final class PropertiesGenerator implements Runnable {
    private final TraitCodegenWriter writer;
    private final Shape shape;
    private final SymbolProvider symbolProvider;

    PropertiesGenerator(TraitCodegenWriter writer, Shape shape, SymbolProvider symbolProvider) {
        this.writer = writer;
        this.shape = shape;
        this.symbolProvider = symbolProvider;
    }

    @Override
    public void run() {
        shape.accept(new PropertyGenerator());
        writer.newLine();
    }

    private final class PropertyGenerator extends ShapeVisitor.Default<Void> {
        private static final String PROPERTY_TEMPLATE = "private final $T $L;";

        @Override
        protected Void getDefault(Shape shape) {
            throw new UnsupportedOperationException("Property generator does not support shape "
                    + shape + " of type " + shape.getType());
        }

        @Override
        public Void listShape(ListShape shape) {
            createValuesProperty(shape);
            return null;
        }

        @Override
        public Void byteShape(ByteShape shape) {
            createValueProperty(shape);
            return null;
        }

        @Override
        public Void shortShape(ShortShape shape) {
            createValueProperty(shape);
            return null;
        }

        @Override
        public Void integerShape(IntegerShape shape) {
            createValueProperty(shape);
            return null;
        }

        @Override
        public Void intEnumShape(IntEnumShape shape) {
            writer.addImport(Integer.class);
            writer.writeWithNoFormatting("private final Integer value;");
            for (Map.Entry<String, MemberShape> memberEntry : shape.getAllMembers().entrySet()) {
                writer.pushState(new EnumVariantSection(memberEntry.getValue()));
                writer.write("public static final Integer $L = $L;", memberEntry.getKey(),
                        memberEntry.getValue().expectTrait(EnumValueTrait.class).expectIntValue());
                writer.popState();
            }

            return null;
        }

        @Override
        public Void longShape(LongShape shape) {
            createValueProperty(shape);
            return null;
        }

        @Override
        public Void floatShape(FloatShape shape) {
            createValueProperty(shape);
            return null;
        }

        @Override
        public Void documentShape(DocumentShape shape) {
            // Document shapes have no properties
            return null;
        }

        @Override
        public Void doubleShape(DoubleShape shape) {
            createValueProperty(shape);
            return null;
        }

        @Override
        public Void bigIntegerShape(BigIntegerShape shape) {
            createValueProperty(shape);
            return null;
        }

        @Override
        public Void bigDecimalShape(BigDecimalShape shape) {
            createValueProperty(shape);
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            createValuesProperty(shape);
            return null;
        }

        @Override
        public Void stringShape(StringShape shape) {
            createValueProperty(shape);
            return null;
        }

        @Override
        public Void enumShape(EnumShape shape) {
            for (Map.Entry<String, MemberShape> memberEntry : shape.getAllMembers().entrySet()) {
                writer.pushState(new EnumVariantSection(memberEntry.getValue()));
                writer.write("public static final String $L = $S;", memberEntry.getKey(),
                        memberEntry.getValue().expectTrait(EnumValueTrait.class).expectStringValue());
                writer.popState();
            }
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            for (MemberShape member : shape.members()) {
                writer.write(PROPERTY_TEMPLATE, symbolProvider.toSymbol(member), symbolProvider.toMemberName(member));
            }
            return null;
        }

        private void createValueProperty(Shape shape) {
            writer.write("private final $B value;", symbolProvider.toSymbol(shape));
        }

        private void createValuesProperty(Shape shape) {
            writer.write("private final $B values;", symbolProvider.toSymbol(shape));
        }
    }
}
