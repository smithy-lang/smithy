/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators;

import java.util.Iterator;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.EnumShape;
import software.amazon.smithy.model.shapes.IntEnumShape;
import software.amazon.smithy.model.shapes.ListShape;
import software.amazon.smithy.model.shapes.MapShape;
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.traitcodegen.Mapper;
import software.amazon.smithy.traitcodegen.SymbolProperties;
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
        writer.writeDocStringContents("@throws software.amazon.smithy.model.node.ExpectationNotMetException "
                + "if the given Node is invalid.");
        writer.closeDocstring();

        // Write actual method
        writer.openBlock("public static $T fromNode($T node) {", "}",
                symbol, Node.class, () -> shape.accept(new FromNodeBodyGenerator()));
        writer.newLine();
    }

    /**
     * Generates the mapping from a node member to a builder field.
     */
    private static final class MemberGenerator extends ShapeVisitor.Default<Void> {
        private final MemberShape member;
        private final TraitCodegenWriter writer;
        private final Model model;
        private final SymbolProvider symbolProvider;

        private MemberGenerator(MemberShape member, TraitCodegenWriter writer, Model model,
                                SymbolProvider symbolProvider) {
            this.member = member;
            this.writer = writer;
            this.model = model;
            this.symbolProvider = symbolProvider;
        }


        @Override
        public Void memberShape(MemberShape shape) {
            return model.expectShape(shape.getTarget()).accept(this);
        }

        @Override
        protected Void getDefault(Shape shape) {
            writer.writeInline(getMemberPrefix() + "$C",
                    symbolProvider.toSymbol(shape).expectProperty(SymbolProperties.MEMBER_MAPPER, Mapper.class)
                            .with(symbolProvider.toMemberName(member))
            );
            return null;
        }

        @Override
        public Void listShape(ListShape shape) {
            writer.writeInline(getMemberPrefix() + "ArrayMember($1S, n -> $3C, builder::$2L)",
                    member.getMemberName(), symbolProvider.toMemberName(member),
                    symbolProvider.toSymbol(shape.getMember()).expectProperty(SymbolProperties.FROM_NODE_MAPPER,
                    Mapper.class).with("n"));
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            Mapper keyMapper = symbolProvider.toSymbol(shape.getKey())
                    .expectProperty(SymbolProperties.FROM_NODE_MAPPER, Mapper.class);
            Mapper valueMapper = symbolProvider.toSymbol(shape.getValue())
                    .expectProperty(SymbolProperties.FROM_NODE_MAPPER, Mapper.class);
            writer.disableNewlines();
            writer.openBlock(getMemberPrefix()
                            + "ObjectMember($S, o -> o.getMembers().forEach((k, v) -> {\n", "}))",
                    member.getMemberName(),
                    () -> writer.write("builder.put$L($C, $C);\n",
                            StringUtils.capitalize(symbolProvider.toMemberName(member)),
                            keyMapper.with("k"), valueMapper.with("v")));
            writer.enableNewlines();
            return null;
        }

        @Override
        public Void stringShape(StringShape shape) {
            if (TraitCodegenUtils.isJavaString(symbolProvider.toSymbol(shape))) {
                writer.writeInline(getMemberPrefix() + "StringMember($S, builder::$L)",
                        member.getMemberName(), symbolProvider.toMemberName(member));
            } else {
                generateGenericMember(shape);
            }
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            generateGenericMember(shape);
            return null;
        }

        private void generateGenericMember(Shape shape) {
            writer.writeInline(getMemberPrefix() + "Member($1S, n -> $3C, builder::$2L)",
                    member.getMemberName(), symbolProvider.toMemberName(member),
                    symbolProvider.toSymbol(shape)
                            .expectProperty(SymbolProperties.FROM_NODE_MAPPER, Mapper.class).with("n"));
        }

        private String getMemberPrefix() {
            return member.isRequired() ? ".expect" : ".get";
        }
    }

    private final class FromNodeBodyGenerator extends ShapeVisitor.Default<Void> {
        private static final String BUILDER_INITIALIZER = "Builder builder = builder();";
        private static final String BUILD_AND_RETURN = "return builder.build();";

        @Override
        protected Void getDefault(Shape shape) {
            throw new UnsupportedOperationException("From Node Generator does not support shape "
                    + shape + " of type " + shape.getType());
        }

        @Override
        public Void listShape(ListShape shape) {
            Symbol memberSymbol = symbolProvider.toSymbol(shape.getMember());
            writer.writeWithNoFormatting(BUILDER_INITIALIZER);
            writer.writeWithNoFormatting("node.expectArrayNode()");
            writer.indent();
            writer.writeWithNoFormatting(".getElements().stream()");
            writer.write(".map(n -> $C)",
                    memberSymbol.expectProperty(SymbolProperties.FROM_NODE_MAPPER, Mapper.class).with("n"));
            writer.writeWithNoFormatting(".forEach(builder::addValues);");
            writer.dedent();
            writer.writeWithNoFormatting(BUILD_AND_RETURN);

            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            writer.writeWithNoFormatting(BUILDER_INITIALIZER);
            Symbol keySymbol = symbolProvider.toSymbol(shape.getKey());
            Symbol valueSymbol = symbolProvider.toSymbol(shape.getValue());
            writer.openBlock("node.expectObjectNode().getMembers().forEach((k, v) -> {", "});",
                    () -> writer.write("builder.putValues($C, $C);",
                            keySymbol.expectProperty(SymbolProperties.FROM_NODE_MAPPER, Mapper.class).with("k"),
                            valueSymbol.expectProperty(SymbolProperties.FROM_NODE_MAPPER, Mapper.class).with("v")));
            writer.writeWithNoFormatting(BUILD_AND_RETURN);
            return null;
        }

        @Override
        public Void intEnumShape(IntEnumShape shape) {
            writer.write("return $T.valueOf(node.expectNumberNode().getValue().intValue());", symbol);
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
            writer.write(BUILDER_INITIALIZER);
            // If the shape has no members (i.e. is an annotation trait) then there will be no member setters, and we
            // need to terminate the line.
            writer.putContext("isEmpty", shape.members().isEmpty());
            writer.write("node.expectObjectNode()${?isEmpty};${/isEmpty}");
            writer.indent();
            Iterator<MemberShape> memberIterator = shape.members().iterator();
            while (memberIterator.hasNext()) {
                MemberShape member = memberIterator.next();
                member.accept(new MemberGenerator(member, writer, model, symbolProvider));
                if (memberIterator.hasNext()) {
                    writer.writeInlineWithNoFormatting("\n");
                } else {
                    writer.writeWithNoFormatting(";\n");
                }
            }
            writer.dedent();
            writer.write(BUILD_AND_RETURN);

            return null;
        }
    }
}
