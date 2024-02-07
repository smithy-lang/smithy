/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators;

import java.util.EnumSet;
import java.util.Optional;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.FloatShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.IntegerShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.LongShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.traitcodegen.sections.GetterSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.StringUtils;

/**
 * Generates getter methods for each shape member or the value type held by the trait.
 *
 * <p>Optional member getters will return the member type wrapped in an {@code Optional<T>}.
 */
final class GetterGenerator implements Runnable {
    private static final EnumSet<ShapeType> NO_OPTIONAL_WRAPPING_TYPES = EnumSet.of(ShapeType.MAP, ShapeType.LIST);
    private final TraitCodegenWriter writer;
    private final SymbolProvider symbolProvider;
    private final Shape shape;
    private final Model model;

    GetterGenerator(TraitCodegenWriter writer, SymbolProvider symbolProvider, Shape shape, Model model) {
        this.writer = writer;
        this.symbolProvider = symbolProvider;
        this.shape = shape;
        this.model = model;
    }

    @Override
    public void run() {
        shape.accept(new GetterVisitor());
    }

    public final class GetterVisitor extends ShapeVisitor.Default<Void> {
        @Override
        protected Void getDefault(Shape shape) {
            // Do not generate a getter by default
            return null;
        }

        @Override
        public Void shortShape(ShortShape shape) {
            generateValueGetter(shape);
            return null;
        }

        @Override
        public Void integerShape(IntegerShape shape) {
            generateValueGetter(shape);
            return null;
        }

        @Override
        public Void longShape(LongShape shape) {
            generateValueGetter(shape);
            return null;
        }

        @Override
        public Void floatShape(FloatShape shape) {
            generateValueGetter(shape);
            return null;
        }

        @Override
        public Void doubleShape(DoubleShape shape) {
            generateValueGetter(shape);
            return null;
        }

        @Override
        public Void bigIntegerShape(BigIntegerShape shape) {
            generateValueGetter(shape);
            return null;
        }

        @Override
        public Void bigDecimalShape(BigDecimalShape shape) {
            generateValueGetter(shape);
            return null;
        }

        @Override
        public Void listShape(ListShape shape) {
            generateValuesGetter(shape);
            return null;
        }

        @Override
        public Void byteShape(ByteShape shape) {
            generateValueGetter(shape);
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            generateValuesGetter(shape);
            return null;
        }

        @Override
        public Void stringShape(StringShape shape) {
            generateValueGetter(shape);
            return null;
        }

        @Override
        public Void intEnumShape(IntEnumShape shape) {
            writer.addImport(Integer.class);
            writer.pushState(new GetterSection(shape));
            writer.openBlock("public Integer getValue() {", "}",
                    () -> writer.write("return value;"));
            writer.popState();
            writer.newLine();
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            for (MemberShape member : shape.members()) {
                // If the member is required or the type does not require an optional wrapper (such as a list or map)
                // then do not wrap return in an Optional
                if (member.isRequired()
                        || NO_OPTIONAL_WRAPPING_TYPES.contains(model.expectShape(member.getTarget()).getType())) {
                    generateNonOptionalGetter(member);
                } else {
                    generateOptionalGetter(member);
                }
                writer.newLine();
            }
            return null;
        }

        private void generateNonOptionalGetter(MemberShape member) {
            writer.pushState(new GetterSection(member));
            writer.openBlock("public $T get$L() {", "}",
                    symbolProvider.toSymbol(member), StringUtils.capitalize(symbolProvider.toMemberName(member)),
                    () -> writer.write("return $L;", symbolProvider.toMemberName(member)));
            writer.popState();
            writer.newLine();
        }

        private void generateOptionalGetter(MemberShape member) {
            writer.addImport(Optional.class);
            writer.pushState(new GetterSection(member));
            writer.openBlock("public Optional<$T> get$L() {", "}",
                    symbolProvider.toSymbol(member), StringUtils.capitalize(symbolProvider.toMemberName(member)),
                    () -> writer.write("return Optional.ofNullable($L);", symbolProvider.toMemberName(member)));
            writer.popState();
            writer.newLine();
        }

        private void generateValuesGetter(Shape shape) {
            writer.pushState(new GetterSection(shape));
            writer.openBlock("public $B getValues() {", "}",
                    symbolProvider.toSymbol(shape), () -> writer.write("return values;"));
            writer.popState();
            writer.newLine();
        }

        private void generateValueGetter(Shape shape) {
            writer.pushState(new GetterSection(shape));
            writer.openBlock("public $B getValue() {", "}",
                    symbolProvider.toSymbol(shape), () -> writer.write("return value;"));
            writer.popState();
            writer.newLine();
        }
    }
}
