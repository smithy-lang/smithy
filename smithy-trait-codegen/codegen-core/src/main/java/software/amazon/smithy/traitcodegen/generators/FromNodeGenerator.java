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
import software.amazon.smithy.traitcodegen.SymbolProperties;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
import software.amazon.smithy.traitcodegen.sections.FromNodeSection;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.StringUtils;

/**
 * Generates the static {@code fromNode} method to deserialize a smithy node into an instance of a Java class.
 */
final class FromNodeGenerator implements Runnable {
    private static final String FROM_NODE_METHOD_TEMPLATE = "public static $T fromNode(Node node) {";

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
        writer.pushState(new FromNodeSection(symbol));
        writer.addImport(Node.class);
        writer.openBlock(FROM_NODE_METHOD_TEMPLATE, "}", symbol, () -> shape.accept(new FromNodeBodyGenerator()));
        writer.popState();
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
            writer.writeInline(getMemberPrefix()
                            + symbolProvider.toSymbol(shape).expectProperty(SymbolProperties.MEMBER_MAPPER),
                    symbolProvider.toMemberName(member)
            );
            return null;
        }

        @Override
        public Void listShape(ListShape shape) {
            writer.writeInline(getMemberPrefix() + "ArrayMember($S, n -> "
                    + symbolProvider.toSymbol(shape.getMember()).expectProperty(SymbolProperties.FROM_NODE_MAPPER,
                    String.class)
                    + ", builder::$L)", symbolProvider.toMemberName(member), "n", symbolProvider.toMemberName(member));
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            String keyMapper =
                    symbolProvider.toSymbol(shape.getKey()).expectProperty(SymbolProperties.FROM_NODE_MAPPER,
                            String.class);
            String valueMapper =
                    symbolProvider.toSymbol(shape.getValue()).expectProperty(SymbolProperties.FROM_NODE_MAPPER,
                            String.class);
            writer.disableNewlines();
            writer.openBlock(getMemberPrefix()
                            + "ObjectMember($S, o -> o.getMembers().forEach((k, v) -> {\n", "}))",
                    symbolProvider.toMemberName(member),
                    () -> writer.write("builder.put$L(" + keyMapper + ", " + valueMapper + ");\n",
                            StringUtils.capitalize(symbolProvider.toMemberName(member)), "k", "v"));
            writer.enableNewlines();
            return null;
        }

        @Override
        public Void stringShape(StringShape shape) {
            if (TraitCodegenUtils.isJavaString(symbolProvider.toSymbol(shape))) {
                writer.writeInline(getMemberPrefix() + "StringMember($1S, builder::$1L)",
                        symbolProvider.toMemberName(member));
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
            writer.writeInline(getMemberPrefix() + "Member($S, n -> "
                    + symbolProvider.toSymbol(shape).expectProperty(SymbolProperties.FROM_NODE_MAPPER, String.class)
                    + ", builder::$L)", symbolProvider.toMemberName(member), "n", symbolProvider.toMemberName(member));
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
            writer.write(BUILDER_INITIALIZER);
            writer.write("node.expectArrayNode()");
            writer.indent();
            writer.write(".getElements().stream()");
            writer.write(".map(n -> " + memberSymbol.expectProperty(SymbolProperties.FROM_NODE_MAPPER) + ")", "n");
            writer.write(".forEach(builder::addValuesItem);");
            writer.dedent();
            writer.write(BUILD_AND_RETURN);

            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            writer.write(BUILDER_INITIALIZER);
            Symbol keySymbol = symbolProvider.toSymbol(shape.getKey());
            Symbol valueSymbol = symbolProvider.toSymbol(shape.getValue());
            writer.openBlock("node.expectObjectNode().getMembers().forEach((k, v) -> {", "});",
                    () -> writer.write("builder.putValues("
                                    + keySymbol.expectProperty(SymbolProperties.FROM_NODE_MAPPER, String.class) + ", "
                                    + valueSymbol.expectProperty(SymbolProperties.FROM_NODE_MAPPER, String.class)
                                    + ");",
                            "k", "v"));
            writer.write(BUILD_AND_RETURN);
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
                    writer.writeInline("\n");
                } else {
                    writer.writeInline(";\n");
                }
            }
            writer.dedent();
            writer.write(BUILD_AND_RETURN);

            return null;
        }
    }
}
