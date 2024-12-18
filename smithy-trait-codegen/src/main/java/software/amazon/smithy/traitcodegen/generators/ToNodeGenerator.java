/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.generators;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.Map;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NumberNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.BooleanShape;
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
import software.amazon.smithy.model.traits.IdRefTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.TraitDefinition;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

/**
 * Generates methods to serialize a Java class to a smithy {@code Node}.
 *
 * <p>If the shape this generator is targeting is a trait then the serialization method is
 * called {@code createNode()}, otherwise the method generated is called {@code toNode()}.
 * This is because Trait classes inherit from {@link software.amazon.smithy.model.traits.AbstractTrait}
 * which requires that they override {@code createNode()} for serialization.
 */
final class ToNodeGenerator implements Runnable {

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
        writer.openBlock(shape.hasTrait(TraitDefinition.class) ? "protected $T createNode() {" : "public $T toNode() {",
                "}",
                Node.class,
                () -> shape.accept(new CreateNodeBodyGenerator()));
        writer.newLine();
    }

    private final class CreateNodeBodyGenerator extends TraitVisitor<Void> {

        @Override
        public Void listShape(ListShape shape) {
            writer.write("return values.stream()")
                    .indent()
                    .write(".map(s -> $C)",
                            (Runnable) () -> shape.getMember().accept(new ToNodeMapperVisitor("s")))
                    .write(".collect($T.collect(getSourceLocation()));", ArrayNode.class)
                    .dedent();
            return null;
        }

        @Override
        public Void documentShape(DocumentShape shape) {
            writer.writeWithNoFormatting("throw new UnsupportedOperationException(\"NodeCache is always set\");");
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            // If it is a Map<string,string> use a simpler syntax
            if (TraitCodegenUtils.isJavaString(symbolProvider.toSymbol(shape.getKey()))
                    && TraitCodegenUtils.isJavaString(symbolProvider.toSymbol(shape.getValue()))) {
                writer.write("return $T.fromStringMap(values).toBuilder()", ObjectNode.class)
                        .writeWithNoFormatting(".sourceLocation(getSourceLocation()).build();");
                return null;
            }
            writer.writeWithNoFormatting("return values.entrySet().stream()")
                    .indent()
                    .write(".map(entry -> new $T<>(", AbstractMap.SimpleImmutableEntry.class)
                    .indent()
                    .write("$C, $C))",
                            (Runnable) () -> shape.getKey()
                                    .accept(
                                            new ToNodeMapperVisitor("entry.getKey()")),
                            (Runnable) () -> shape.getValue()
                                    .accept(
                                            new ToNodeMapperVisitor("entry.getValue()")))
                    .dedent()
                    .write(".collect($1T.collect($2T::getKey, $2T::getValue))",
                            ObjectNode.class,
                            Map.Entry.class)
                    .writeWithNoFormatting(".toBuilder().sourceLocation(getSourceLocation()).build();")
                    .dedent();
            return null;
        }

        @Override
        public Void stringShape(StringShape shape) {
            if (TraitCodegenUtils.isJavaString(symbolProvider.toSymbol(shape))) {
                return null;
            }
            toStringCreator();
            return null;
        }

        @Override
        public Void enumShape(EnumShape shape) {
            if (shape.hasTrait(TraitDefinition.class)) {
                writer.write("return $T.from(value);", Node.class);
            } else {
                toStringCreator();
            }
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
            for (MemberShape mem : shape.members()) {
                if (TraitCodegenUtils.isNullableMember(mem)) {
                    writer.write(".withOptionalMember($S, get$U().map(m -> $C))",
                            mem.getMemberName(),
                            symbolProvider.toMemberName(mem),
                            (Runnable) () -> mem.accept(new ToNodeMapperVisitor("m")));
                } else {
                    writer.write(".withMember($S, $C)",
                            mem.getMemberName(),
                            (Runnable) () -> mem.accept(new ToNodeMapperVisitor(symbolProvider.toMemberName(mem))));
                }
            }
            writer.writeWithNoFormatting(".build();");
            writer.dedent();
            return null;
        }

        @Override
        protected Void numberShape(NumberShape shape) {
            writer.write("return new $T(value, getSourceLocation());", NumberNode.class);
            return null;
        }

        @Override
        public Void timestampShape(TimestampShape shape) {
            if (shape.hasTrait(TimestampFormatTrait.class)) {
                switch (shape.expectTrait(TimestampFormatTrait.class).getFormat()) {
                    case EPOCH_SECONDS:
                        writer.write("return new $T(value.getEpochSecond(), getSourceLocation());",
                                NumberNode.class);
                        break;
                    case HTTP_DATE:
                        writer.write("return new $T($T.RFC_1123_DATE_TIME.format(",
                                StringNode.class,
                                DateTimeFormatter.class);
                        writer.indent();
                        writer.write("$T.ofInstant(value, $T.UTC)), getSourceLocation());",
                                ZonedDateTime.class,
                                ZoneOffset.class);
                        writer.dedent();
                        break;
                    default:
                        toStringCreator();
                        break;
                }
            } else {
                toStringCreator();
            }

            return null;
        }

        private void toStringCreator() {
            writer.write("return new $T(value.toString(), getSourceLocation());", StringNode.class);
        }
    }

    /**
     * Determines how to map a shape to a node.
     */
    private final class ToNodeMapperVisitor extends TraitVisitor<Void> {
        private final String varName;

        ToNodeMapperVisitor(String varName) {
            this.varName = varName;
        }

        @Override
        public Void stringShape(StringShape shape) {
            if (shape.hasTrait(IdRefTrait.class)) {
                toStringMapper();
            } else {
                fromNodeMapper();
            }
            return null;
        }

        @Override
        public Void booleanShape(BooleanShape shape) {
            fromNodeMapper();
            return null;
        }

        @Override
        public Void listShape(ListShape shape) {
            writer.write("$L.stream().map(s -> $C).collect($T.collect())",
                    varName,
                    (Runnable) () -> shape.getMember().accept(new ToNodeMapperVisitor("s")),
                    ArrayNode.class);
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            writer.openBlock("$L.entrySet().stream()",
                    "",
                    varName,
                    () -> writer.write(".map(entry -> new $T<>(", AbstractMap.SimpleImmutableEntry.class)
                            .indent()
                            .write("$C, $C))",
                                    (Runnable) () -> shape.getKey()
                                            .accept(
                                                    new ToNodeMapperVisitor("entry.getKey()")),
                                    (Runnable) () -> shape.getValue()
                                            .accept(
                                                    new ToNodeMapperVisitor("entry.getValue()")))
                            .dedent()
                            .write(".collect($1T.collect($2T::getKey, $2T::getValue))",
                                    ObjectNode.class,
                                    Map.Entry.class));
            return null;
        }

        @Override
        public Void memberShape(MemberShape shape) {
            if (shape.hasTrait(IdRefTrait.class)) {
                toStringMapper();
            } else {
                model.expectShape(shape.getTarget()).accept(this);
            }
            return null;
        }

        @Override
        protected Void numberShape(NumberShape shape) {
            fromNodeMapper();
            return null;
        }

        @Override
        public Void intEnumShape(IntEnumShape shape) {
            writer.write("$L.toNode()", varName);
            return null;
        }

        @Override
        public Void documentShape(DocumentShape shape) {
            fromNodeMapper();
            return null;
        }

        @Override
        public Void enumShape(EnumShape shape) {
            writer.write("$T.from($L.getValue())", Node.class, varName);
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            writer.write("$L.toNode()", varName);
            return null;
        }

        @Override
        public Void timestampShape(TimestampShape shape) {
            if (shape.hasTrait(TimestampFormatTrait.class)) {
                switch (shape.expectTrait(TimestampFormatTrait.class).getFormat()) {
                    case EPOCH_SECONDS:
                        writer.write("$T.from($L.getEpochSecond())", Node.class, varName);
                        return null;
                    case HTTP_DATE:
                        writer.write("$T.from($T.RFC_1123_DATE_TIME.format($L))",
                                Node.class,
                                DateTimeFormatter.class,
                                varName);
                        return null;
                    default:
                        // Fall through on default
                        break;
                }
            }
            toStringMapper();
            return null;
        }

        private void fromNodeMapper() {
            writer.write("$T.from($L)", Node.class, varName);
        }

        private void toStringMapper() {
            writer.write("$T.from($L.toString())", Node.class, varName);
        }
    }
}
