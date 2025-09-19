/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.generators;

import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.StringNode;
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
import software.amazon.smithy.model.traits.IdRefTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.model.traits.UniqueItemsTrait;
import software.amazon.smithy.traitcodegen.SymbolProperties;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

/**
 * Generates the static {@code fromNode} method to deserialize a smithy node into an instance of a Java class.
 */
final class FromNodeGenerator extends TraitVisitor<Void> implements Runnable {
    private final TraitCodegenWriter writer;
    private final Symbol symbol;
    private final Shape shape;
    private final SymbolProvider symbolProvider;
    private final Model model;

    FromNodeGenerator(
            TraitCodegenWriter writer,
            Symbol symbol,
            Shape shape,
            SymbolProvider symbolProvider,
            Model model
    ) {
        this.writer = writer;
        this.symbol = symbol;
        this.shape = shape;
        this.symbolProvider = symbolProvider;
        this.model = model;
    }

    @Override
    public void run() {
        shape.accept(this);
    }

    @Override
    public Void listShape(ListShape shape) {
        if (TraitCodegenUtils.isJavaStringList(shape, symbolProvider)) {
            return null;
        }

        writeFromNodeJavaDoc();
        writer.openBlock("public static $T fromNode($T node) {",
                "}",
                symbol,
                Node.class,
                () -> {
                    writer.writeWithNoFormatting("Builder builder = builder()" +
                            (shape.hasTrait(TraitDefinition.ID) ? ".sourceLocation(node);" : ";"));
                    shape.accept(new FromNodeMapperVisitor(writer, model, "node", symbolProvider));
                    writer.write("builder.values(value0);");
                    writer.writeWithNoFormatting("return builder.build();");
                });
        writer.newLine();

        return null;
    }

    @Override
    public Void mapShape(MapShape shape) {
        writeFromNodeJavaDoc();
        writer.openBlock("public static $T fromNode($T node) {",
                "}",
                symbol,
                Node.class,
                () -> {
                    writer.writeWithNoFormatting("Builder builder = builder()" +
                            (shape.hasTrait(TraitDefinition.ID) ? ".sourceLocation(node);" : ";"));
                    shape.accept(new FromNodeMapperVisitor(writer, model, "node", symbolProvider));
                    writer.writeWithNoFormatting("return builder.build();");
                });
        writer.newLine();

        return null;
    }

    @Override
    public Void documentShape(DocumentShape shape) {
        return null;
    }

    @Override
    public Void stringShape(StringShape shape) {
        return null;
    }

    @Override
    public Void enumShape(EnumShape shape) {
        // Enum traits do not need this method, only nested enums.
        if (symbol.getProperty(SymbolProperties.BASE_SYMBOL).isPresent()) {
            return null;
        }
        writeFromNodeJavaDoc();
        writer.openBlock("public static $T fromNode($T node) {", "}", symbol, Node.class, () -> {
            writer.write("return from(node.expectStringNode().getValue());");
        });
        writer.newLine();
        return null;
    }

    @Override
    public Void intEnumShape(IntEnumShape shape) {
        // Enum traits do not need this method, only nested enums.
        if (symbol.getProperty(SymbolProperties.BASE_SYMBOL).isPresent()) {
            return null;
        }
        writeFromNodeJavaDoc();
        writer.openBlock("public static $T fromNode($T node) {", "}", symbol, Node.class, () -> {
            writer.writeWithNoFormatting("return from(node.expectNumberNode().getValue().intValue());");
        });
        writer.newLine();
        return null;
    }

    @Override
    protected Void numberShape(NumberShape shape) {
        // Number shapes do not create a from node method
        return null;
    }

    @Override
    public Void structureShape(StructureShape shape) {
        writeFromNodeJavaDoc();
        writer.write("public static $T fromNode($T node) {", symbol, Node.class);
        writer.indent();
        writer.write("Builder builder = builder()" +
                (shape.hasTrait(TraitDefinition.ID) ? ".sourceLocation(node);" : ";"));
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
                writer.writeInlineWithNoFormatting(";\n");
            }
        }
        writer.dedent();
        writer.write("return builder.build();");
        writer.dedent().write("}");
        writer.newLine();

        return null;
    }

    @Override
    public Void timestampShape(TimestampShape shape) {
        writeFromNodeJavaDoc();
        writer.openBlock("public static $T fromNode($T node) {",
                "}",
                symbol,
                Node.class,
                this::writeTimestampDeser);
        writer.newLine();

        return null;
    }

    @Override
    public Void unionShape(UnionShape shape) {
        writer.write("public static $T fromNode($T node) {", symbol, Node.class);
        writer.indent();
        writer.write("$T<$T, $T> members = node.expectObjectNode().getMembers();",
                Map.class,
                StringNode.class,
                Node.class);
        writer.openBlock("if (members.size() != 1) {",
                "}",
                () -> {
                    writer.write("throw new $T($S);",
                            IllegalArgumentException.class,
                            "Expected only one member for union node.");
                });
        writer.write("$T<$T, $T> value = members.entrySet().iterator().next();",
                Map.Entry.class,
                StringNode.class,
                Node.class);
        writer.openBlock("switch (value.getKey().getValue()) {",
                "}",
                () -> {
                    Set<String> tags = shape.getAllMembers().keySet();
                    for (String tag : tags) {
                        String memberName = tag + "Member";
                        writer.write("case $S:", tag);
                        writer.indent();
                        writer.write("return $U.from(value.getValue());", memberName);
                        writer.dedent();
                    }
                    writer.writeWithNoFormatting("default:");
                    writer.indent();
                    writer.write("throw new $T($S);", IllegalArgumentException.class, "Unknown union variant.");
                    writer.dedent();
                });
        writer.dedent();
        writer.write("}");
        writer.newLine();
        return null;
    }

    private void writeTimestampDeser() {
        // If the trait has the timestamp format trait then we only need a single
        // way to deserialize the input node. Without the timestamp format trait
        // timestamp should be able to handle both epoch seconds and date time formats.
        if (shape.hasTrait(TimestampFormatTrait.ID)) {
            writer.write("return new $T($C, node.getSourceLocation());",
                    symbol,
                    (Runnable) () -> shape.accept(new FromNodeMapperVisitor(writer, model, "node", symbolProvider)));
        } else {
            writer.openBlock("if (node.isNumberNode()) {", "}", () -> {
                writer.write("return new $T($T.ofEpochSecond(node.expectNumberNode().getValue().longValue()),",
                        symbol,
                        Instant.class).indent();
                writer.writeWithNoFormatting("node.getSourceLocation());").dedent();
            });
            writer.write("return new $T($T.parse(node.expectStringNode().getValue()), node.getSourceLocation());",
                    symbol,
                    Instant.class);
        }
        writer.newLine();
    }

    private void writeFromNodeJavaDoc() {
        writer.writeDocString(writer.format("Creates a {@link $1T} from a {@link Node}.\n\n"
                + "@param node Node to create the $1T from.\n"
                + "@return Returns the created $1T.\n"
                + "@throws $2T if the given Node is invalid.\n",
                symbol,
                ExpectationNotMetException.class));
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
            this.memberPrefix = (member.isRequired() && !member.hasNonNullDefault()) ? ".expect" : ".get";
        }

        @Override
        public Void memberShape(MemberShape shape) {
            if (shape.hasTrait(IdRefTrait.ID) || shape.hasTrait(TimestampFormatTrait.ID)) {
                writeObjectNodeMember(shape);
                return null;
            }
            return model.expectShape(shape.getTarget()).accept(this);
        }

        @Override
        public Void booleanShape(BooleanShape shape) {
            writer.writeInline(memberPrefix + "BooleanMember($S, builder::$L)", fieldName, memberName);
            return null;
        }

        @Override
        public Void listShape(ListShape shape) {
            writer.writeInline(memberPrefix + "ArrayMember($1S, n -> ", fieldName);

            Shape memberTarget = model.expectShape(shape.getMember().getTarget());
            boolean isMemberCollection = memberTarget.isListShape() || memberTarget.isMapShape();
            if (isMemberCollection) {
                writer.indent();
                writer.writeInline("{\n$C\n",
                        (Runnable) () -> shape.getMember()
                                .accept(new FromNodeMapperVisitor(writer, model, "n", 1, symbolProvider)));
                writer.writeInlineWithNoFormatting("return value1;\n");
                writer.dedent();
                writer.writeInlineWithNoFormatting("}, ");
            } else {
                writer.writeInline("$C, ",
                        (Runnable) () -> shape.getMember()
                                .accept(new FromNodeMapperVisitor(writer, model, "n", 1, symbolProvider)));
            }

            boolean isSet = shape.hasTrait(UniqueItemsTrait.ID);
            if (isSet) {
                writer.writeInline("l -> builder.$L(new $T<>(l)))", fieldName, LinkedHashSet.class);
            } else {
                writer.writeInline("builder::$L)", fieldName);
            }
            return null;
        }

        @Override
        public Void byteShape(ByteShape shape) {
            writer.writeInline(memberPrefix + "NumberMember($S, n -> builder.$L(n.byteValue()))",
                    fieldName,
                    memberName);
            return null;
        }

        @Override
        public Void shortShape(ShortShape shape) {
            writer.writeInline(memberPrefix + "NumberMember($S, n -> builder.$L(n.shortValue()))",
                    fieldName,
                    memberName);
            return null;
        }

        @Override
        public Void integerShape(IntegerShape shape) {
            writer.writeInline(memberPrefix + "NumberMember($S, n -> builder.$L(n.intValue()))",
                    fieldName,
                    memberName);
            return null;
        }

        @Override
        public Void longShape(LongShape shape) {
            writer.writeInline(memberPrefix + "NumberMember($S, n -> builder.$L(n.longValue()))",
                    fieldName,
                    memberName);
            return null;
        }

        @Override
        public Void floatShape(FloatShape shape) {
            writer.writeInline(memberPrefix + "NumberMember($S, n -> builder.$L(n.floatValue()))",
                    fieldName,
                    memberName);
            return null;
        }

        @Override
        public Void documentShape(DocumentShape shape) {
            writer.writeInline(memberPrefix + "Member($1S, $3T::expectObjectNode, builder::$2L)",
                    fieldName,
                    memberName,
                    Node.class);
            return null;
        }

        @Override
        public Void doubleShape(DoubleShape shape) {
            writer.writeInline(memberPrefix + "NumberMember($S, n -> builder.$L(n.doubleValue()))",
                    fieldName,
                    memberName);
            return null;
        }

        @Override
        public Void bigIntegerShape(BigIntegerShape shape) {
            writer.writeInline(memberPrefix
                    + "Member($S, n -> n.expectNumberNode().asBigDecimal().get().toBigInteger(), builder::$L)",
                    fieldName,
                    memberName);
            return null;
        }

        @Override
        public Void bigDecimalShape(BigDecimalShape shape) {
            writer.writeInline(memberPrefix
                    + "Member($S, n -> n.expectNumberNode().asBigDecimal().get(), builder::$L)",
                    fieldName,
                    memberName);
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            writer.writeInline(memberPrefix + "ObjectMember($S, o -> {\n", fieldName);
            writer.indent();
            writer.writeInline("$C\n",
                    (Runnable) () -> shape.accept(new FromNodeMapperVisitor(writer, model, "o", 1, symbolProvider)));
            writer.writeInline("builder.$L(value1);", memberName);
            writer.dedent();
            writer.writeInlineWithNoFormatting("\n})");
            return null;
        }

        @Override
        public Void intEnumShape(IntEnumShape shape) {
            writer.writeInline(memberPrefix + "NumberMember($S, n -> builder.$L($T.from(n.intValue())))",
                    fieldName,
                    memberName,
                    symbolProvider.toSymbol(shape));
            return null;
        }

        @Override
        public Void stringShape(StringShape shape) {
            if (TraitCodegenUtils.isJavaString(symbolProvider.toSymbol(shape))) {
                writer.writeInline(memberPrefix + "StringMember($S, builder::$L)", fieldName, memberName);
            } else {
                writer.writeInline(memberPrefix + "Member($1S, n -> $3C, builder::$2L)",
                        fieldName,
                        memberName,
                        (Runnable) () -> shape.accept(new FromNodeMapperVisitor(writer, model, "n", symbolProvider)));
            }
            return null;
        }

        @Override
        public Void enumShape(EnumShape shape) {
            writer.writeInline(memberPrefix + "StringMember($S, n -> builder.$L($T.from(n)))",
                    fieldName,
                    memberName,
                    symbolProvider.toSymbol(shape));
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            writeObjectNodeMember(shape);
            return null;
        }

        @Override
        public Void timestampShape(TimestampShape shape) {
            writeObjectNodeMember(shape);
            return null;
        }

        @Override
        public Void unionShape(UnionShape shape) {
            writeObjectNodeMember(shape);
            return null;
        }

        @Override
        public Void blobShape(BlobShape shape) {
            throw new UnsupportedOperationException("Shape not supported " + shape);
        }

        private void writeObjectNodeMember(Shape shape) {
            writer.writeInline(memberPrefix + "Member($1S, n -> $3C, builder::$2L)",
                    fieldName,
                    memberName,
                    (Runnable) () -> shape.accept(new FromNodeMapperVisitor(writer, model, "n", symbolProvider)));
        }
    }
}
