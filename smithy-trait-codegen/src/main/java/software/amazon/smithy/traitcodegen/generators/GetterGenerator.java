/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators;

import java.util.Optional;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.DocumentShape;
import software.amazon.smithy.model.shapes.DoubleShape;
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
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.traitcodegen.sections.GetterSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.StringUtils;

/**
 * Generates getter methods for each shape member or the value type held by the trait.
 *
 * <p>Optional member getters will return the member type wrapped in an {@code Optional<T>}.
 */
final class GetterGenerator implements Runnable {
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

    public final class GetterVisitor extends ShapeVisitor.DataShapeVisitor<Void> {

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
        public Void documentShape(DocumentShape shape) {
            // Documents do not generate any getters. You must
            // use the .toNode method to get the value of a document trait.
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
        public Void blobShape(BlobShape shape) {
            generateValueGetter(shape);
            return null;
        }

        @Override
        public Void booleanShape(BooleanShape shape) {
            throw new UnsupportedOperationException("Boolean shapes not supported for trait code generation. "
                    + "Consider using an Annotation (empty structure) trait instead");
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
            writer.pushState(new GetterSection(shape));
            writer.openBlock("public $T getValue() {", "}",
                    Integer.class, () -> writer.writeWithNoFormatting("return value;"));
            writer.popState();
            writer.newLine();
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            for (MemberShape member : shape.members()) {
                // If the member is required or the type does not require an optional wrapper (such as a list or map)
                // then do not wrap return in an Optional
                if (member.isRequired()) {
                    generateNonOptionalGetter(member);
                } else {
                    generateOptionalGetter(member);
                }
                writer.newLine();
            }
            return null;
        }

        @Override
        public Void unionShape(UnionShape shape) {
            throw new UnsupportedOperationException("Union Shapes are not supported at this time.");
        }

        @Override
        public Void timestampShape(TimestampShape shape) {
            throw new UnsupportedOperationException("Timestamp Shapes are not supported at this time.");
        }

        @Override
        public Void memberShape(MemberShape shape) {
            throw new IllegalArgumentException("Cannot generate a getter for Member shape: " + shape);
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
            writer.pushState(new GetterSection(member));
            writer.openBlock("public $T<$T> get$L() {", "}",
                    Optional.class, symbolProvider.toSymbol(member),
                    StringUtils.capitalize(symbolProvider.toMemberName(member)),
                    () -> writer.write("return $T.ofNullable($L);",
                            Optional.class, symbolProvider.toMemberName(member)));
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
