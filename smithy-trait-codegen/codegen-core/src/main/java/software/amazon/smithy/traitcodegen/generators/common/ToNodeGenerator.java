/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.traitcodegen.generators.common;

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
import software.amazon.smithy.traitcodegen.SymbolProperties;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;
import software.amazon.smithy.utils.SmithyInternalApi;
import software.amazon.smithy.utils.StringUtils;

/**
 * Generates methods to serialize a Java class to a smithy {@code Node}.
 * <p>
 * If the shape this generator is targeting is a trait then the serialization method is
 * called {@code createNode()}, otherwise the method generated is called {@code toNode()}.
 * This is because Trait classes inherit from {@link software.amazon.smithy.model.traits.AbstractTrait}
 * which requires that they override {@code createNode()} for serialization.
 */
@SmithyInternalApi
public final class ToNodeGenerator implements Runnable {
    private static final String CREATE_NODE_METHOD = "protected Node createNode() {";
    private static final String TO_NODE_METHOD = "public Node toNode() {";

    private final TraitCodegenWriter writer;
    private final Shape shape;
    private final SymbolProvider symbolProvider;
    private final Model model;

    public ToNodeGenerator(TraitCodegenWriter writer, Shape shape, SymbolProvider symbolProvider, Model model) {
        this.writer = writer;
        this.shape = shape;
        this.symbolProvider = symbolProvider;
        this.model = model;
    }

    @Override
    public void run() {
        writer.addImport(Node.class);
        writer.override();
        writer.openBlock(shape.hasTrait(TraitDefinition.class) ? CREATE_NODE_METHOD : TO_NODE_METHOD, "}",
                () -> shape.accept(new CreateNodeBodyGenerator()));
        writer.newLine();
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
            symbol.getProperty(SymbolProperties.NODE_MAPPING_IMPORTS, Symbol.class).ifPresent(writer::addImport);
            writer.addImport(ArrayNode.class);
            writer.write("return values.stream()")
                    .indent()
                    .write(".map(s -> " + symbol.expectProperty(SymbolProperties.TO_NODE_MAPPER, String.class) + ")",
                            "s")
                    .write(".collect(ArrayNode.collect(getSourceLocation()));")
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
            writer.write("throw new UnsupportedOperationException(\"NodeCache is always set\");");
            return null;
        }

        @Override
        public Void doubleShape(DoubleShape shape) {
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
            writer.addImport(ObjectNode.class);
            // If it is a string, string map use the easier syntax
            if (TraitCodegenUtils.isJavaString(symbolProvider.toSymbol(shape.getKey()))
                    && TraitCodegenUtils.isJavaString(symbolProvider.toSymbol(shape.getValue()))
            ) {
                writer.write("return ObjectNode.fromStringMap(values).toBuilder()")
                        .write(".sourceLocation(getSourceLocation()).build();");
                return null;
            }
            Symbol keySymbol = symbolProvider.toSymbol(shape.getKey());
            Symbol valueSymbol = symbolProvider.toSymbol(shape.getValue());
            keySymbol.getProperty(SymbolProperties.NODE_MAPPING_IMPORTS, Symbol.class).ifPresent(writer::addImport);
            valueSymbol.getProperty(SymbolProperties.NODE_MAPPING_IMPORTS, Symbol.class).ifPresent(writer::addImport);
            String keyMapper = keySymbol.expectProperty(SymbolProperties.TO_NODE_MAPPER, String.class);
            String valueMapper = valueSymbol.expectProperty(SymbolProperties.TO_NODE_MAPPER, String.class);

            writer.addImports(AbstractMap.class, Map.class);
            writer.write("return values.entrySet().stream()")
                    .indent()
                    .write(".map(entry -> new AbstractMap.SimpleImmutableEntry<>(")
                    .indent()
                    .write(keyMapper + ", " + valueMapper + "))", "entry.getKey()", "entry.getValue()")
                    .dedent()
                    .write(".collect(ObjectNode.collect(Map.Entry::getKey, Map.Entry::getValue))")
                    .write(".toBuilder().sourceLocation(getSourceLocation()).build();")
                    .dedent();
            return null;
        }

        @Override
        public Void stringShape(StringShape shape) {
            writer.addImport(StringNode.class);
            writer.write("return new StringNode(value.toString(), getSourceLocation());");
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            if (!shape.members().isEmpty()) {
                writer.addImport(Node.class);
                writer.write("return Node.objectNodeBuilder()").indent();
                if (shape.hasTrait(TraitDefinition.class)) {
                    writer.write(".sourceLocation(getSourceLocation())");
                }

                // Generate all members
                // TODO: This is all quite clunky. Fix
                for (MemberShape member : shape.members()) {
                    Symbol memberSymbol = symbolProvider.toSymbol(member);
                    memberSymbol.getProperty(SymbolProperties.NODE_MAPPING_IMPORTS, Symbol.class)
                            .ifPresent(writer::addImport);
                    if (member.isRequired()) {
                        writer.write(".withMember($S, "
                                        + memberSymbol.expectProperty(SymbolProperties.TO_NODE_MAPPER, String.class)
                                        + ")",
                                symbolProvider.toMemberName(member), symbolProvider.toMemberName(member));
                    } else if (model.expectShape(member.getTarget()).isListShape()) {
                        writer.addImport(ArrayNode.class);
                        Symbol listTargetSymbol = symbolProvider.toSymbol(
                                model.expectShape(member.getTarget()).asListShape()
                                        .orElseThrow(RuntimeException::new).getMember());
                        writer.write(".withMember($S, get$L().stream().map(s -> "
                                        + listTargetSymbol.expectProperty(SymbolProperties.TO_NODE_MAPPER, String.class)
                                        + ").collect(ArrayNode.collect()))",
                                symbolProvider.toMemberName(member),
                                StringUtils.capitalize(symbolProvider.toMemberName(member)), "s");
                    } else if (model.expectShape(member.getTarget()).isMapShape()) {
                        MapShape mapShape =
                                model.expectShape(member.getTarget()).asMapShape().orElseThrow(RuntimeException::new);
                        Symbol keySymbol = symbolProvider.toSymbol(mapShape.getKey());
                        Symbol valueSymbol = symbolProvider.toSymbol(mapShape.getValue());
                        keySymbol.getProperty(SymbolProperties.NODE_MAPPING_IMPORTS, Symbol.class)
                                .ifPresent(writer::addImport);
                        valueSymbol.getProperty(SymbolProperties.NODE_MAPPING_IMPORTS, Symbol.class)
                                .ifPresent(writer::addImport);
                        String keyMapper = keySymbol.expectProperty(SymbolProperties.TO_NODE_MAPPER, String.class);
                        String valueMapper = valueSymbol.expectProperty(SymbolProperties.TO_NODE_MAPPER, String.class);

                        writer.addImports(AbstractMap.class, Map.class, ObjectNode.class);
                        writer.openBlock(".withMember($S, get$L().entrySet().stream()", ")",
                                member.getMemberName(),
                                StringUtils.capitalize(member.getMemberName()),
                                () -> writer.write(".map(entry -> new AbstractMap.SimpleImmutableEntry<>(")
                                        .indent()
                                        .write(keyMapper + ", " + valueMapper + "))",
                                                "entry.getKey()", "entry.getValue()")
                                        .dedent()
                                        .write(".collect(ObjectNode.collect(Map.Entry::getKey, Map.Entry::getValue))"));
                    } else {
                        writer.write(".withOptionalMember($S, get$L().map(m -> " + memberSymbol
                                        .expectProperty(SymbolProperties.TO_NODE_MAPPER, String.class) + "))",
                                symbolProvider.toMemberName(member),
                                StringUtils.capitalize(symbolProvider.toMemberName(member)), "m");
                    }
                }
                writer.write(".build();");
                writer.dedent();
            }
            return null;
        }

        private void generateNumberTraitCreator() {
            writer.addImport(NumberNode.class);
            writer.write("return new NumberNode(value, getSourceLocation());");
        }
    }
}
