/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators;

import java.util.AbstractMap;
import java.util.Map;
import software.amazon.smithy.codegen.core.Symbol;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
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
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.traitcodegen.Mapper;
import software.amazon.smithy.traitcodegen.SymbolProperties;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.Pair;
import software.amazon.smithy.utils.StringUtils;

/**
 * Generates methods to serialize a Java class to a smithy {@code Node}.
 *
 * <p>If the shape this generator is targeting is a trait then the serialization method is
 * called {@code createNode()}, otherwise the method generated is called {@code toNode()}.
 * This is because Trait classes inherit from {@link software.amazon.smithy.model.traits.AbstractTrait}
 * which requires that they override {@code createNode()} for serialization.
 */
final class ToNodeGenerator implements Runnable {
    private static final String CREATE_NODE_METHOD = "protected $T createNode() {";
    private static final String TO_NODE_METHOD = "public $T toNode() {";

    private final TraitCodegenWriter writer;
    private final Shape shape;
    private final SymbolProvider symbolProvider;
    private final Model model;

    ToNodeGenerator(TraitCodegenWriter writer, Shape shape, SymbolProvider symbolProvider, Model model) {
        this.writer = writer;
        this.shape = shape;
        this.symbolProvider = symbolProvider;
        this.model = model;
    }

    @Override
    public void run() {
        writer.override();
        writer.openBlock(shape.hasTrait(TraitDefinition.class) ? CREATE_NODE_METHOD : TO_NODE_METHOD, "}",
                Node.class, () -> shape.accept(new CreateNodeBodyGenerator()));
        writer.newLine();
    }

    private Pair<Mapper, Mapper> getKeyValueMappers(MapShape shape) {
        Mapper keyMapper = symbolProvider.toSymbol(shape.getKey())
                .expectProperty(SymbolProperties.TO_NODE_MAPPER, Mapper.class);
        Mapper valueMapper = symbolProvider.toSymbol(shape.getValue())
                .expectProperty(SymbolProperties.TO_NODE_MAPPER, Mapper.class);
        return Pair.of(keyMapper, valueMapper);
    }

    private final class CreateNodeBodyGenerator extends ShapeVisitor.Default<Void> {
        @Override
        protected Void getDefault(Shape shape) {
            throw new UnsupportedOperationException("CreateNodeBodyGenerator does not support shape "
                    + shape + " of type " + shape.getType());
        }

        @Override
        public Void listShape(ListShape shape) {
            Symbol symbol = symbolProvider.toSymbol(shape.getMember());
            writer.write("return values.stream()")
                    .indent()
                    .write(".map(s -> $C)",
                            symbol.expectProperty(SymbolProperties.TO_NODE_MAPPER, Mapper.class).with("s"))
                    .write(".collect($T.collect(getSourceLocation()));", ArrayNode.class)
                    .dedent();
            return null;
        }

        @Override
        public Void byteShape(ByteShape shape) {
            generateNumberTraitCreator();
            return null;
        }

        @Override
        public Void shortShape(ShortShape shape) {
            generateNumberTraitCreator();
            return null;
        }

        @Override
        public Void integerShape(IntegerShape shape) {
            generateNumberTraitCreator();
            return null;
        }

        @Override
        public Void intEnumShape(IntEnumShape shape) {
            generateNumberTraitCreator();
            return null;
        }

        @Override
        public Void longShape(LongShape shape) {
            generateNumberTraitCreator();
            return null;
        }

        @Override
        public Void floatShape(FloatShape shape) {
            generateNumberTraitCreator();
            return null;
        }

        @Override
        public Void documentShape(DocumentShape shape) {
            writer.writeWithNoFormatting("throw new UnsupportedOperationException(\"NodeCache is always set\");");
            return null;
        }

        @Override
        public Void doubleShape(DoubleShape shape) {
            generateNumberTraitCreator();
            return null;
        }

        @Override
        public Void bigIntegerShape(BigIntegerShape shape) {
            generateNumberTraitCreator();
            return null;
        }

        @Override
        public Void bigDecimalShape(BigDecimalShape shape) {
            generateNumberTraitCreator();
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            // If it is a Map<string,string> use a simpler syntax
            if (TraitCodegenUtils.isJavaString(symbolProvider.toSymbol(shape.getKey()))
                    && TraitCodegenUtils.isJavaString(symbolProvider.toSymbol(shape.getValue()))
            ) {
                writer.write("return $T.fromStringMap(values).toBuilder()", ObjectNode.class)
                        .writeWithNoFormatting(".sourceLocation(getSourceLocation()).build();");
                return null;
            }
            Pair<Mapper, Mapper> mappers = getKeyValueMappers(shape);
            writer.writeWithNoFormatting("return values.entrySet().stream()")
                    .indent()
                    .write(".map(entry -> new $T.SimpleImmutableEntry<>(", AbstractMap.class)
                    .indent()
                    .write("$C, $C))",
                            mappers.getLeft().with("entry.getKey()"),
                            mappers.getRight().with("entry.getValue()"))
                    .dedent()
                    .write(".collect($1T.collect($2T.Entry::getKey, $2T.Entry::getValue))",
                            ObjectNode.class, Map.class)
                    .writeWithNoFormatting(".toBuilder().sourceLocation(getSourceLocation()).build();")
                    .dedent();
            return null;
        }

        @Override
        public Void stringShape(StringShape shape) {
            writer.write("return new $T(value.toString(), getSourceLocation());", StringNode.class);
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            writer.write("return $T.objectNodeBuilder()", Node.class).indent();
            if (shape.hasTrait(TraitDefinition.class)) {
                // If the shape is a trait we need to add the source location of trait to the
                // generated node.
                writer.writeInline(".sourceLocation(getSourceLocation())");
            }
            for (MemberShape member : shape.members()) {
                member.accept(new MemberMapperVisitor(symbolProvider.toMemberName(member),
                        symbolProvider, member.isRequired()));
            }
            writer.writeWithNoFormatting(".build();");
            writer.dedent();
            return null;
        }

        private void generateNumberTraitCreator() {
            writer.write("return new $T(value, getSourceLocation());", NumberNode.class);
        }
    }

    // Writes the per-member mapping from a member to an object node member node.
    private final class MemberMapperVisitor extends ShapeVisitor.Default<Void> {
        private final String memberName;
        private final SymbolProvider symbolProvider;
        private final boolean required;

        private MemberMapperVisitor(String memberName,
                                    SymbolProvider symbolProvider,
                                    boolean required
        ) {
            this.memberName = memberName;
            this.symbolProvider = symbolProvider;
            this.required = required;
        }

        @Override
        protected Void getDefault(Shape shape) {
            Symbol symbol = symbolProvider.toSymbol(shape);
            if (required) {
                writer.write(".withMember($S, $C)",
                        memberName,
                        symbol.expectProperty(SymbolProperties.TO_NODE_MAPPER, Mapper.class).with(memberName));
            } else {
                writer.write(".withOptionalMember($S, get$L().map(m -> $C))",
                        memberName, StringUtils.capitalize(memberName),
                        symbol.expectProperty(SymbolProperties.TO_NODE_MAPPER, Mapper.class).with("m"));
            }
            return null;
        }

        @Override
        public Void memberShape(MemberShape shape) {
            return model.expectShape(shape.getTarget()).accept(this);
        }

        @Override
        public Void listShape(ListShape shape) {
            Symbol listTargetSymbol = symbolProvider.toSymbol(shape.getMember());
            writer.write(".withMember($S, get$L().stream().map(s -> $C).collect($T.collect()))",
                    memberName, StringUtils.capitalize(memberName),
                    listTargetSymbol.expectProperty(SymbolProperties.TO_NODE_MAPPER, Mapper.class).with("s"),
                    ArrayNode.class
            );
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            Pair<Mapper, Mapper> mappers = getKeyValueMappers(shape);
            writer.openBlock(".withMember($S, get$L().entrySet().stream()", ")",
                    memberName,
                    StringUtils.capitalize(memberName),
                    () -> writer.write(".map(entry -> new $T.SimpleImmutableEntry<>(", AbstractMap.class)
                            .indent()
                            .write("$C, $C))",
                                    mappers.getLeft().with("entry.getKey()"),
                                    mappers.getRight().with("entry.getValue()"))
                            .dedent()
                            .write(".collect($1T.collect($2T.Entry::getKey, $2T.Entry::getValue))",
                                    ObjectNode.class, Map.class));
            return null;
        }
    }
}
