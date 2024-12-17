/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.generators;

import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.BooleanShape;
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
import software.amazon.smithy.model.shapes.NumberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.traits.UniqueItemsTrait;
import software.amazon.smithy.traitcodegen.SymbolProperties;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.SmithyBuilder;

/**
 * Generates a constructor for a type.
 */
final class ConstructorGenerator extends TraitVisitor<Void> implements Runnable {
    private final TraitCodegenWriter writer;
    private final Symbol symbol;
    private final Shape shape;
    private final SymbolProvider symbolProvider;

    ConstructorGenerator(
            TraitCodegenWriter writer,
            Symbol symbol,
            Shape shape,
            SymbolProvider symbolProvider
    ) {
        this.writer = writer;
        this.symbol = symbol;
        this.shape = shape;
        this.symbolProvider = symbolProvider;
    }

    @Override
    public void run() {
        shape.accept(this);
    }

    @Override
    public Void listShape(ListShape shape) {
        if (!shape.hasTrait(UniqueItemsTrait.class)
                && TraitCodegenUtils.isJavaString(symbolProvider.toSymbol(shape.getMember()))) {
            writer.openBlock("public $1T($1B values, $2T sourceLocation) {",
                    "}",
                    symbol,
                    FromSourceLocation.class,
                    () -> writer.write("super(ID, values, sourceLocation);"));
            writer.newLine();

            writer.openBlock("public $1T($1B values) {",
                    "}",
                    symbol,
                    () -> writer.write("super(ID, values, $T.NONE);", SourceLocation.class));
            writer.newLine();
        } else {
            writeConstructorWithBuilder();
        }

        return null;
    }

    @Override
    public Void mapShape(MapShape shape) {
        writeConstructorWithBuilder();
        return null;
    }

    @Override
    public Void intEnumShape(IntEnumShape shape) {
        Symbol integerSymbol = TraitCodegenUtils.fromClass(Integer.class);
        // Constructor with no source location
        writer.openBlock("public $T($T value) {",
                "}",
                symbol,
                integerSymbol,
                () -> {
                    writer.write("super(ID, $T.NONE);", SourceLocation.class);
                    writer.writeWithNoFormatting("this.value = value;");
                });
        writer.newLine();

        // Constructor with source location
        writer.openBlock("public $T($T value, $T sourceLocation) {",
                "}",
                symbol,
                integerSymbol,
                FromSourceLocation.class,
                () -> {
                    writer.writeWithNoFormatting("super(ID, sourceLocation);");
                    writer.writeWithNoFormatting("this.value = value;");
                });
        writer.newLine();
        return null;
    }

    @Override
    public Void documentShape(DocumentShape shape) {
        writer.openBlock("public $T($T value) {",
                "}",
                symbol,
                Node.class,
                () -> writer.writeWithNoFormatting("super(ID, value);"));
        writer.newLine();
        return null;
    }

    @Override
    public Void stringShape(StringShape shape) {
        if (TraitCodegenUtils.isJavaString(symbol)) {
            writeStringTraitConstructors();
        } else {
            writeValueShapeConstructors();
        }
        return null;
    }

    @Override
    public Void enumShape(EnumShape shape) {
        writeStringTraitConstructors();
        return null;
    }

    @Override
    public Void structureShape(StructureShape shape) {
        writeConstructorWithBuilder();
        return null;
    }

    @Override
    public Void timestampShape(TimestampShape shape) {
        writeValueShapeConstructors();
        return null;
    }

    @Override
    protected Void numberShape(NumberShape shape) {
        writeValueShapeConstructors();
        return null;
    }

    private void writeConstructorWithBuilder() {
        writer.openBlock("private $T(Builder builder) {", "}", symbol, () -> {
            // If the shape is a trait include the source location. Nested shapes don't have a separate source location.
            if (shape.hasTrait(TraitDefinition.class)) {
                writer.writeWithNoFormatting("super(ID, builder.getSourceLocation());");
            }
            shape.accept(new InitializerVisitor());
        });
        writer.newLine();
    }

    private void writeValueShapeConstructors() {
        // Constructor with no source location
        writer.openBlock("public $1T($1B value) {", "}", symbol, () -> {
            writer.write("super(ID, $T.NONE);", SourceLocation.class);
            writer.writeWithNoFormatting("this.value = value;");
        });
        writer.newLine();

        // Constructor with source location
        writer.openBlock("public $1T($1B value, $2T sourceLocation) {",
                "}",
                symbol,
                FromSourceLocation.class,
                () -> {
                    writer.writeWithNoFormatting("super(ID, sourceLocation);");
                    writer.writeWithNoFormatting("this.value = value;");
                });
        writer.newLine();
    }

    private void writeStringTraitConstructors() {
        // Without source location
        writer.openBlock("public $T(String value) {",
                "}",
                symbol,
                () -> writer.write("super(ID, value, $T.NONE);", SourceLocation.class));
        writer.newLine();

        // With source location
        writer.openBlock("public $T($T value, $T sourceLocation) {",
                "}",
                symbol,
                String.class,
                FromSourceLocation.class,
                () -> writer.writeWithNoFormatting("super(ID, value, sourceLocation);"));
        writer.newLine();
    }

    /**
     * Generates the actual field initialization statements for each member of a shape.
     */
    private final class InitializerVisitor extends ShapeVisitor.DataShapeVisitor<Void> {

        @Override
        public Void booleanShape(BooleanShape shape) {
            return null;
        }

        @Override
        public Void listShape(ListShape shape) {
            writeValuesInitializer();
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            writeValuesInitializer();
            return null;
        }

        @Override
        public Void byteShape(ByteShape shape) {
            return null;
        }

        @Override
        public Void shortShape(ShortShape shape) {
            return null;
        }

        @Override
        public Void integerShape(IntegerShape shape) {
            return null;
        }

        @Override
        public Void longShape(LongShape shape) {
            return null;
        }

        @Override
        public Void floatShape(FloatShape shape) {
            return null;
        }

        @Override
        public Void documentShape(DocumentShape shape) {
            return null;
        }

        @Override
        public Void doubleShape(DoubleShape shape) {
            return null;
        }

        @Override
        public Void bigIntegerShape(BigIntegerShape shape) {
            return null;
        }

        @Override
        public Void bigDecimalShape(BigDecimalShape shape) {
            return null;
        }

        @Override
        public Void stringShape(StringShape shape) {
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            for (MemberShape member : shape.members()) {
                if (TraitCodegenUtils.isNullableMember(member)) {
                    writer.write("this.$L = $L;", symbolProvider.toMemberName(member), getBuilderValue(member));
                } else {
                    writer.write("this.$1L = $2T.requiredState($1S, $3L);",
                            symbolProvider.toMemberName(member),
                            SmithyBuilder.class,
                            getBuilderValue(member));
                }
            }
            return null;
        }

        @Override
        public Void unionShape(UnionShape shape) {
            throw new UnsupportedOperationException("Does not support shape of type " + shape.getType());
        }

        @Override
        public Void blobShape(BlobShape shape) {
            throw new UnsupportedOperationException("Does not support shape of type " + shape.getType());
        }

        @Override
        public Void memberShape(MemberShape shape) {
            throw new UnsupportedOperationException("Does not support shape of type " + shape.getType());
        }

        @Override
        public Void timestampShape(TimestampShape shape) {
            return null;
        }

        private String getBuilderValue(MemberShape member) {
            String memberName = symbolProvider.toMemberName(member);

            // If the member requires a builderRef we need to copy that builder ref value rather than use it directly.
            if (symbolProvider.toSymbol(member).getProperty(SymbolProperties.BUILDER_REF_INITIALIZER).isPresent()) {
                if (TraitCodegenUtils.isNullableMember(member)) {
                    return writer.format("builder.$1L.hasValue() ? builder.$1L.copy() : null", memberName);
                } else {
                    return writer.format("builder.$1L.copy()", memberName);
                }
            }
            return writer.format("builder.$L", memberName);
        }

        private void writeValuesInitializer() {
            writer.writeWithNoFormatting("this.values = builder.values.copy();");
        }
    }
}
