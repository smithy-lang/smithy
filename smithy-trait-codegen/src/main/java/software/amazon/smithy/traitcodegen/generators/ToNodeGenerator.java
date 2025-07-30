/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.generators;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
        writer.openBlock(shape.hasTrait(TraitDefinition.ID) ? "protected $T createNode() {" : "public $T toNode() {",
                "}",
                Node.class,
                () -> shape.accept(new CreateNodeBodyGenerator()));
        writer.newLine();
    }

    private final class CreateNodeBodyGenerator extends TraitVisitor<Void> {

        @Override
        public Void listShape(ListShape shape) {
            writer.write("$C",
                    (Runnable) () -> shape.accept(new ToNodeMapperVisitor("values", 0, symbolProvider)));
            writer.writeWithNoFormatting("return builder0.sourceLocation(getSourceLocation()).build();");
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
            writer.write("$C",
                    (Runnable) () -> shape.accept(new ToNodeMapperVisitor("values", 0, symbolProvider)));
            writer.writeWithNoFormatting("return builder0.sourceLocation(getSourceLocation()).build();");
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
            if (shape.hasTrait(TraitDefinition.ID)) {
                writer.write("return $T.from(value);", Node.class);
            } else {
                toStringCreator();
            }
            return null;
        }

        @Override
        public Void structureShape(StructureShape shape) {
            writer.write("return $T.objectNodeBuilder()", Node.class).indent();
            if (shape.hasTrait(TraitDefinition.ID)) {
                // If the shape is a trait we need to add the source location of trait to the
                // generated node.
                writer.writeWithNoFormatting(".sourceLocation(getSourceLocation())");
            }
            for (MemberShape member : shape.members()) {
                Shape memTarget = model.expectShape(member.getTarget());
                boolean isMemCollection = memTarget.isMapShape() || memTarget.isListShape();
                boolean isNullable = TraitCodegenUtils.isNullableMember(member);
                String memberName = member.getMemberName();
                String getterName = symbolProvider.toMemberName(member);
                if (isMemCollection) {
                    generateCollectionMember(member, memberName, getterName, isNullable);
                } else {
                    generateSimpleMember(member, memberName, getterName, isNullable);
                }
            }
            writer.writeWithNoFormatting(".build();");
            writer.dedent();
            return null;
        }

        private void generateCollectionMember(
                MemberShape member,
                String memberName,
                String getterName,
                boolean isNullable
        ) {
            if (isNullable) {
                writer.write(".withOptionalMember($S, get$U().map(m -> {",
                        memberName,
                        getterName);
                writer.indent();
                writer.write("$C",
                        (Runnable) () -> member.accept(
                                new ToNodeMapperVisitor("m", 1, symbolProvider)));
            } else {
                writer.write(".withMember($S, () -> {", memberName);
                writer.indent();
                writer.write("$C",
                        (Runnable) () -> member.accept(
                                new ToNodeMapperVisitor(getterName, 1, symbolProvider)));
            }

            writer.writeWithNoFormatting("return builder1.build();");

            writer.dedent();
            writer.writeWithNoFormatting(isNullable ? "}))" : "})");
        }

        private void generateSimpleMember(
                MemberShape member,
                String memberName,
                String getterName,
                boolean isNullable
        ) {
            if (isNullable) {
                writer.write(".withOptionalMember($S, get$U().map(m -> $C))",
                        memberName,
                        getterName,
                        (Runnable) () -> member.accept(new ToNodeMapperVisitor(
                                "m",
                                1,
                                symbolProvider)));
            } else {
                writer.write(".withMember($S, $C)",
                        memberName,
                        (Runnable) () -> member.accept(new ToNodeMapperVisitor(
                                getterName,
                                1,
                                symbolProvider)));
            }
        }

        @Override
        protected Void numberShape(NumberShape shape) {
            writer.write("return new $T(value, getSourceLocation());", NumberNode.class);
            return null;
        }

        @Override
        public Void timestampShape(TimestampShape shape) {
            if (shape.hasTrait(TimestampFormatTrait.ID)) {
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
        private final int nestedLevel;
        private final SymbolProvider symbolProvider;

        ToNodeMapperVisitor(String varName, int nestedLevel, SymbolProvider symbolProvider) {
            this.varName = varName;
            this.nestedLevel = nestedLevel;
            this.symbolProvider = symbolProvider;
        }

        @Override
        public Void stringShape(StringShape shape) {
            if (shape.hasTrait(IdRefTrait.ID)) {
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
            writer.write("$1T.Builder $2L = $1T.builder();",
                    ArrayNode.class,
                    "builder" + nestedLevel);

            Shape memberTarget = model.expectShape(shape.getMember().getTarget());
            int nextLevel = nestedLevel + 1;
            writer.write("for ($T $L : $L) {",
                    symbolProvider.toSymbol(memberTarget),
                    "element" + nextLevel,
                    varName);
            writer.indent();

            if (memberTarget.isListShape() || memberTarget.isMapShape()) {
                writer.write("$C",
                        (Runnable) () -> shape.getMember()
                                .accept(new ToNodeMapperVisitor(
                                        "element" + nextLevel,
                                        nextLevel,
                                        symbolProvider)));
                writer.write("$L.withValue($L.build());",
                        "builder" + nestedLevel,
                        "builder" + nextLevel);
            } else {
                writer.write("$L.withValue($C);",
                        "builder" + nestedLevel,
                        (Runnable) () -> shape.getMember()
                                .accept(new ToNodeMapperVisitor(
                                        "element" + nextLevel,
                                        nextLevel,
                                        symbolProvider)));
            }
            writer.dedent();
            writer.writeWithNoFormatting("}");
            return null;
        }

        @Override
        public Void mapShape(MapShape shape) {
            writer.write("$1T.Builder $2L = $1T.builder();",
                    ObjectNode.class,
                    "builder" + nestedLevel);
            int nextLevel = nestedLevel + 1;
            writer.write("for ($T<$T, $T> $L : $L.entrySet()) {",
                    Map.Entry.class,
                    String.class,
                    symbolProvider.toSymbol(shape.getValue()),
                    "entry" + nextLevel,
                    varName);
            writer.indent();
            writer.write("$T $L = $C;",
                    StringNode.class,
                    "key" + nextLevel,
                    (Runnable) () -> shape.getKey()
                            .accept(new ToNodeMapperVisitor(
                                    "entry" + nextLevel + ".getKey()",
                                    nextLevel,
                                    symbolProvider)));
            Shape valueTarget = model.expectShape(shape.getValue().getTarget());
            if (valueTarget.isListShape() || valueTarget.isMapShape()) {
                writer.write("$C",
                        (Runnable) () -> shape.getValue()
                                .accept(new ToNodeMapperVisitor(
                                        "entry" + nextLevel + ".getValue()",
                                        nextLevel,
                                        symbolProvider)));
                writer.write("$L.withMember($L, $L.build());",
                        "builder" + nestedLevel,
                        "key" + nextLevel,
                        "builder" + nextLevel);
            } else {
                writer.write("$L.withMember($L, $C);",
                        "builder" + nestedLevel,
                        "key" + nextLevel,
                        (Runnable) () -> shape.getValue()
                                .accept(new ToNodeMapperVisitor(
                                        "entry" + nextLevel + ".getValue()",
                                        nextLevel,
                                        symbolProvider)));
            }
            writer.dedent();
            writer.writeWithNoFormatting("}");
            return null;
        }

        @Override
        public Void memberShape(MemberShape shape) {
            if (shape.hasTrait(IdRefTrait.ID)) {
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
            if (shape.hasTrait(TimestampFormatTrait.ID)) {
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
