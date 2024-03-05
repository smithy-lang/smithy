/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators;

import java.util.Iterator;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ExpectationNotMetException;
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
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.StringUtils;

/**
 * Generates the static {@code fromNode} method to deserialize a smithy node into an instance of a Java class.
 */
final class FromNodeGenerator implements Runnable {
    private final TraitCodegenWriter writer;
    private final Symbol symbol;
    private final Shape shape;
    private final SymbolProvider symbolProvider;
    private final Model model;

    FromNodeGenerator(TraitCodegenWriter writer, Symbol symbol, Shape shape, SymbolProvider symbolProvider,
                      Model model) {
        this.writer = writer;
        this.symbol = symbol;
        this.shape = shape;
        this.symbolProvider = symbolProvider;
        this.model = model;
    }

    @Override
    public void run() {
        // Add docstring for method
        writer.openDocstring();
        writer.writeDocStringContents("Creates a {@link $T} from a {@link Node}.", symbol);
        writer.writeDocStringContents("");
        writer.writeDocStringContents("@param node Node to create the $T from.", symbol);
        writer.writeDocStringContents("@return Returns the created $T.", symbol);
        writer.writeDocStringContents("@throws $T if the given Node is invalid.",
                ExpectationNotMetException.class);
        writer.closeDocstring();

        // Write actual method
        writer.openBlock("public static $T fromNode($T node) {", "}",
                symbol, Node.class, () -> shape.accept(new FromNodeBodyGenerator()));
        writer.newLine();
    }

    private final class FromNodeBodyGenerator extends ShapeVisitor.Default<Void> {

        @Override
        protected Void getDefault(Shape shape) {
            throw new UnsupportedOperationException("From Node Generator does not support shape "
                    + shape + " of type " + shape.getType());
        }

        @Override
        public Void listShape(ListShape shape) {
            writer.writeWithNoFormatting("Builder builder = builder();");
            shape.accept(new FromNodeMapperVisitor(writer, model, "node"));
            writer.writeWithNoFormatting("return builder.build();");

            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            writer.writeWithNoFormatting("Builder builder = builder();");
            shape.accept(new FromNodeMapperVisitor(writer, model, "node"));
            writer.writeWithNoFormatting("return builder.build();");
            return null;
        }

        @Override
        public Void intEnumShape(IntEnumShape shape) {
            writer.write("return $T.from(node.expectNumberNode().getValue().intValue());", symbol);
            return null;
        }

        @Override
        public Void enumShape(EnumShape shape) {
            writer.openBlock("return $T.valueOf(node.expectStringNode()", ");", symbol, () -> {
                writer.putContext("enumVariants", shape.getEnumValues());
                writer.write(".expectOneOf(${#enumVariants}${key:S}${^key.last},${/key.last}${/enumVariants})");
            });
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            writer.write("Builder builder = builder();");
            // If the shape has no members (i.e. is an annotation trait) then there will be no member setters, and we
            // need to terminate the line.
            writer.putContext("isEmpty", shape.members().isEmpty());
            writer.write("node.expectObjectNode()${?isEmpty};${/isEmpty}");
            writer.indent();
            Iterator<MemberShape> memberIterator = shape.members().iterator();
            while (memberIterator.hasNext()) {
                MemberShape member = memberIterator.next();
                member.accept(new MemberGenerator(member));
                if (memberIterator.hasNext()) {
                    writer.writeInlineWithNoFormatting("\n");
                } else {
                    writer.writeWithNoFormatting(";\n");
                }
            }
            writer.dedent();
            writer.write("return builder.build();");

            return null;
        }
    }

    /**
     * Generates the mapping from a node member to a builder field.
     */
    private final class MemberGenerator extends ShapeVisitor.DataShapeVisitor<Void> {
        private final String memberName;
        private final String fieldName;
        private final String memberPrefix;

        private MemberGenerator(MemberShape member) {
            this.fieldName = member.getMemberName();
            this.memberName = symbolProvider.toMemberName(member);
            this.memberPrefix = member.isRequired() ? ".expect" : ".get";
        }

        @Override
        public Void memberShape(MemberShape shape) {
            return model.expectShape(shape.getTarget()).accept(this);
        }

        @Override
        public Void booleanShape(BooleanShape shape) {
            writer.writeInline(memberPrefix + "BooleanMember($S, builder::$L)", fieldName, memberName);
            return null;
        }

        @Override
        public Void listShape(ListShape shape) {
            writer.writeInline(memberPrefix + "ArrayMember($1S, n -> $3C, builder::$2L)",
                    fieldName, memberName,
                    (Runnable) () -> shape.getMember().accept(new FromNodeMapperVisitor(writer, model, "n")));
            return null;
        }

        @Override
        public Void byteShape(ByteShape shape) {
            writer.writeInline(memberPrefix + "NumberMember($S, n -> builder.$L(n.byteValue()))",
                    fieldName, memberName);
            return null;
        }

        @Override
        public Void shortShape(ShortShape shape) {
            writer.writeInline(memberPrefix + "NumberMember($S, n -> builder.$L(n.shortValue()))",
                    fieldName, memberName);
            return null;
        }

        @Override
        public Void integerShape(IntegerShape shape) {
            writer.writeInline(memberPrefix + "NumberMember($S, n -> builder.$L(n.intValue()))",
                    fieldName, memberName);
            return null;
        }

        @Override
        public Void longShape(LongShape shape) {
            writer.writeInline(memberPrefix + "NumberMember($S, n -> builder.$L(n.longValue()))",
                    fieldName, memberName);
            return null;
        }

        @Override
        public Void floatShape(FloatShape shape) {
            writer.writeInline(memberPrefix + "NumberMember($S, n -> builder.$L(n.floatValue()))",
                    fieldName, memberName);
            return null;
        }

        @Override
        public Void documentShape(DocumentShape shape) {
            writer.writeInline(memberPrefix + "Member($1S, $3T::expectObjectNode, builder::$2L)",
                    fieldName, memberName, Node.class);
            return null;
        }

        @Override
        public Void doubleShape(DoubleShape shape) {
            writer.writeInline(memberPrefix + "NumberMember($S, n -> builder.$L(n.doubleValue()))",
                    fieldName, memberName);
            return null;
        }

        @Override
        public Void bigIntegerShape(BigIntegerShape shape) {
            writer.writeInline(memberPrefix
                            + "Member($S, n -> n.expectNumberNode().asBigDecimal().get().toBigInteger(), builder::$L)",
                    fieldName, memberName);
            return null;
        }

        @Override
        public Void bigDecimalShape(BigDecimalShape shape) {
            writer.writeInline(memberPrefix
                            + "Member($S, n -> n.expectNumberNode().asBigDecimal().get(), builder::$L)",
                    fieldName, memberName);
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            writer.disableNewlines();
            writer.openBlock(memberPrefix
                            + "ObjectMember($S, o -> o.getMembers().forEach((k, v) -> {\n", "}))",
                    fieldName,
                    () -> writer.write("builder.put$L($C, $C);\n",
                            StringUtils.capitalize(memberName),
                            (Runnable) () -> shape.getKey().accept(new FromNodeMapperVisitor(writer, model, "k")),
                            (Runnable) () -> shape.getValue().accept(new FromNodeMapperVisitor(writer, model, "v")))
            );
            writer.enableNewlines();
            return null;
        }

        @Override
        public Void intEnumShape(IntEnumShape shape) {
            writer.writeInline(memberPrefix + "NumberMember($S, n -> builder.$L($T.from(n.intValue())))",
                    fieldName, memberName, symbolProvider.toSymbol(shape));
            return null;
        }

        @Override
        public Void stringShape(StringShape shape) {
            if (TraitCodegenUtils.isJavaString(symbolProvider.toSymbol(shape))) {
                writer.writeInline(memberPrefix + "StringMember($S, builder::$L)", fieldName, memberName);
            } else {
                writer.writeInline(memberPrefix + "Member($1S, n -> $3C, builder::$2L)",
                        fieldName, memberName,
                        (Runnable) () -> shape.accept(new FromNodeMapperVisitor(writer, model, "n"))
                );
            }
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            writer.writeInline(memberPrefix + "Member($1S, n -> $3C, builder::$2L)",
                    fieldName, memberName,
                    (Runnable) () -> shape.accept(new FromNodeMapperVisitor(writer, model, "n"))
            );
            return null;
        }

        @Override
        public Void timestampShape(TimestampShape shape) {
            writer.writeInline(memberPrefix + "Member($1S, n -> $3C, builder::$2L)",
                    fieldName, memberName,
                    (Runnable) () -> shape.accept(new FromNodeMapperVisitor(writer, model, "n")));
            return null;
        }

        @Override
        public Void unionShape(UnionShape shape) {
            throw new UnsupportedOperationException("Shape not supported " + shape);
        }

        @Override
        public Void blobShape(BlobShape shape) {
            throw new UnsupportedOperationException("Shape not supported " + shape);
        }
    }
}
