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
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.IdRefTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

final class ToNodeMapperVisitor extends TraitVisitor<Void> {
    private final TraitCodegenWriter writer;
    private final Model model;
    private final String varName;
    private final int nestedLevel;
    private final SymbolProvider symbolProvider;

    ToNodeMapperVisitor(
            TraitCodegenWriter writer,
            Model model,
            String varName,
            int nestedLevel,
            SymbolProvider symbolProvider
    ) {
        this.writer = writer;
        this.model = model;
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
                            .accept(new ToNodeMapperVisitor(writer,
                                    model,
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
                            .accept(new ToNodeMapperVisitor(writer,
                                    model,
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
                        .accept(new ToNodeMapperVisitor(writer,
                                model,
                                "entry" + nextLevel + ".getKey()",
                                nextLevel,
                                symbolProvider)));
        Shape valueTarget = model.expectShape(shape.getValue().getTarget());
        if (valueTarget.isListShape() || valueTarget.isMapShape()) {
            writer.write("$C",
                    (Runnable) () -> shape.getValue()
                            .accept(new ToNodeMapperVisitor(writer,
                                    model,
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
                            .accept(new ToNodeMapperVisitor(writer,
                                    model,
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
        } else if (shape.hasTrait(TimestampFormatTrait.ID)) {
            writeForTimestampFormat(shape);
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
            writeForTimestampFormat(shape);
        } else {
            toStringMapper();
        }
        return null;
    }

    @Override
    public Void unionShape(UnionShape shape) {
        writer.write("$L.toNode()", varName);
        return null;
    }

    private void fromNodeMapper() {
        writer.write("$T.from($L)", Node.class, varName);
    }

    private void toStringMapper() {
        writer.write("$T.from($L.toString())", Node.class, varName);
    }

    private void writeForTimestampFormat(Shape shape) {
        switch (shape.expectTrait(TimestampFormatTrait.class).getFormat()) {
            case EPOCH_SECONDS:
                writer.write("$T.from($L.getEpochSecond())", Node.class, varName);
                break;
            case HTTP_DATE:
                writer.write("$T.from($T.RFC_1123_DATE_TIME.format($T.ofInstant($L, $T.UTC)))",
                        Node.class,
                        DateTimeFormatter.class,
                        ZonedDateTime.class,
                        varName,
                        ZoneOffset.class);
                break;
            default:
                // Fall through on default
                toStringMapper();
        }
    }
}
