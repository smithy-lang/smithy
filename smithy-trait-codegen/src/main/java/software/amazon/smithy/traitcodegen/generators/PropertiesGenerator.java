/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.generators;

import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.shapes.DocumentShape;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.NumberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

/**
 * Generates properties for a Java class from Smithy shape members.
 *
 * <p>The generated properties hold the value types of member shapes or a value property representing
 * the data the trait holds. In the following two cases the generated property has a static name:
 * <dl>
 *     <dt>Value Shapes (numbers, enum, strings)</dt>
 *     <dd>property {@code "value"} represents the single data type held by the trait such as a {@code int} value.</dd>
 *     <dt>List and Map shapes</dt>
 *     <dd>property {@code "values"} represents the collection held by the trait such as a list of strings.</dd>
 * </dl>
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

    private final class PropertyGenerator extends TraitVisitor<Void> {

        @Override
        public Void listShape(ListShape shape) {
            // Do not create a property if the shape can inherit from the StringListTrait base class.
            if (TraitCodegenUtils.isJavaStringList(shape, symbolProvider)) {
                return null;
            }
            createValuesProperty(shape);
            return null;
        }

        @Override
        public Void intEnumShape(IntEnumShape shape) {
            writer.write("private final $T value;", Integer.class);
            return null;
        }

        @Override
        public Void documentShape(DocumentShape shape) {
            // Document traits have no properties
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            createValuesProperty(shape);
            return null;
        }

        @Override
        public Void stringShape(StringShape shape) {
            // Only create a value property if the shape is not a java string.
            // If it is a string it will use the value from the StringTrait base class.
            if (!TraitCodegenUtils.isJavaString(symbolProvider.toSymbol(shape))) {
                createValueProperty(shape);
            }
            return null;
        }

        @Override
        public Void enumShape(EnumShape shape) {
            // Enum shapes have no properties
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            for (MemberShape member : shape.members()) {
                writer.write("private final $T $L;",
                        symbolProvider.toSymbol(member),
                        symbolProvider.toMemberName(member));
            }
            return null;
        }

        @Override
        public Void timestampShape(TimestampShape shape) {
            createValueProperty(shape);
            return null;
        }

        @Override
        protected Void numberShape(NumberShape shape) {
            createValueProperty(shape);
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
