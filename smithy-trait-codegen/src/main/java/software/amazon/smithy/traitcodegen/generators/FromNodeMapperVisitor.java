/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.generators;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.codegen.core.SymbolProvider;
import software.amazon.smithy.model.Model;
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
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.IdRefTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.model.traits.UniqueItemsTrait;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

/**
 * Determines how to map a node to a shape.
 */
final class FromNodeMapperVisitor extends ShapeVisitor.DataShapeVisitor<Void> {

    private final TraitCodegenWriter writer;
    private final Model model;
    private final String varName;
    private final SymbolProvider symbolProvider;
    private final int nestedLevel;

    FromNodeMapperVisitor(TraitCodegenWriter writer, Model model, String varName, SymbolProvider symbolProvider) {
        this(writer, model, varName, 0, symbolProvider);
    }

    FromNodeMapperVisitor(
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
    public Void booleanShape(BooleanShape shape) {
        writer.write("$L.expectBooleanNode().getValue()", varName);
        return null;
    }

    @Override
    public Void listShape(ListShape shape) {
        boolean isSet = shape.hasTrait(UniqueItemsTrait.ID);
        writer.write("$T<$T> $L = $L.expectArrayNode().getElements();",
                List.class,
                Node.class,
                "elements" + nestedLevel,
                varName);
        writer.write("$T<$T> $L = new $T<>();",
                isSet ? Set.class : List.class,
                symbolProvider.toSymbol(shape.getMember()),
                "value" + nestedLevel,
                isSet ? LinkedHashSet.class : ArrayList.class);

        writer.write("for ($T $L : $L) {", Node.class, "node" + nestedLevel, "elements" + nestedLevel);
        writer.indent();
        int nextLevel = nestedLevel + 1;
        Shape memberTarget = model.expectShape(shape.getMember().getTarget());
        if (memberTarget.isListShape() || memberTarget.isMapShape()) {
            writer.write("$C",
                    (Runnable) () -> shape.getMember()
                            .accept(new FromNodeMapperVisitor(writer,
                                    model,
                                    "node" + nestedLevel,
                                    nextLevel,
                                    symbolProvider)));
        } else {
            writer.write("$T $L = $C;",
                    symbolProvider.toSymbol(shape.getMember()),
                    "value" + nextLevel,
                    (Runnable) () -> shape.getMember()
                            .accept(new FromNodeMapperVisitor(writer,
                                    model,
                                    "node" + nestedLevel,
                                    nextLevel,
                                    symbolProvider)));
        }

        writer.write("$L.add($L);", "value" + nestedLevel, "value" + nextLevel);
        writer.dedent();
        writer.writeWithNoFormatting("}");
        return null;
    }

    @Override
    public Void mapShape(MapShape shape) {
        // Map traits use putValues() to update entries.So no need to create a new map.
        if (nestedLevel > 0) {
            writer.write("$T<$T, $T> $L = new $T<>();",
                    Map.class,
                    String.class,
                    symbolProvider.toSymbol(shape.getValue()),
                    "value" + nestedLevel,
                    LinkedHashMap.class);
        }
        writer.write("$T<$T, $T> $L = $L.expectObjectNode().getMembers();",
                Map.class,
                StringNode.class,
                Node.class,
                "members" + nestedLevel,
                varName);

        writer.write("for ($T<$T, $T> $L : $L.entrySet()) {",
                Map.Entry.class,
                StringNode.class,
                Node.class,
                "entry" + nestedLevel,
                "members" + nestedLevel);
        writer.indent();
        int nextLevel = nestedLevel + 1;
        Shape valueTarget = model.expectShape(shape.getValue().getTarget());
        // Map value processing
        if (valueTarget.isMapShape() || valueTarget.isListShape()) {
            writer.write("$C",
                    (Runnable) () -> shape.getValue()
                            .accept(new FromNodeMapperVisitor(writer,
                                    model,
                                    "entry" + nestedLevel + ".getValue()",
                                    nextLevel,
                                    symbolProvider)));
        } else {
            writer.write("$T $L = $C;",
                    symbolProvider.toSymbol(shape.getValue()),
                    "value" + nextLevel,
                    (Runnable) () -> shape.getValue()
                            .accept(new FromNodeMapperVisitor(writer,
                                    model,
                                    "entry" + nestedLevel + ".getValue()",
                                    nextLevel,
                                    symbolProvider)));
        }
        // Map key processing
        writer.write("$T $L = $C;",
                symbolProvider.toSymbol(shape.getKey()),
                "key" + nextLevel,
                (Runnable) () -> shape.getKey()
                        .accept(new FromNodeMapperVisitor(writer,
                                model,
                                "entry" + nestedLevel + ".getKey()",
                                nextLevel,
                                symbolProvider)));

        if (nestedLevel > 0) {
            writer.write("$L.put($L, $L);",
                    "value" + nestedLevel,
                    "key" + nextLevel,
                    "value" + nextLevel);
        } else {
            writer.writeWithNoFormatting("builder.putValues(key1, value1);");
        }

        writer.dedent();
        writer.writeWithNoFormatting("}");
        return null;
    }

    @Override
    public Void byteShape(ByteShape shape) {
        writer.write("$L.expectNumberNode().getValue().byteValue()", varName);
        return null;
    }

    @Override
    public Void shortShape(ShortShape shape) {
        writer.write("$L.expectNumberNode().getValue().shortValue()", varName);
        return null;
    }

    @Override
    public Void integerShape(IntegerShape shape) {
        writer.write("$L.expectNumberNode().getValue().intValue()", varName);
        return null;
    }

    @Override
    public Void longShape(LongShape shape) {
        writer.write("$L.expectNumberNode().getValue().longValue()", varName);
        return null;
    }

    @Override
    public Void floatShape(FloatShape shape) {
        writer.write("$L.expectNumberNode().getValue().floatValue()", varName);
        return null;
    }

    @Override
    public Void documentShape(DocumentShape shape) {
        writer.writeWithNoFormatting(varName);
        return null;
    }

    @Override
    public Void doubleShape(DoubleShape shape) {
        writer.write("$L.expectNumberNode().getValue().doubleValue()", varName);
        return null;
    }

    @Override
    public Void bigIntegerShape(BigIntegerShape shape) {
        writer.write("$L.expectNumberNode().asBigDecimal().get().toBigInteger()", varName);
        return null;
    }

    @Override
    public Void bigDecimalShape(BigDecimalShape shape) {
        writer.write("$L.expectNumberNode().asBigDecimal().get()", varName);
        return null;
    }

    @Override
    public Void stringShape(StringShape shape) {
        if (shape.hasTrait(IdRefTrait.ID)) {
            writer.write("$T.fromNode($L)", ShapeId.class, varName);
        } else {
            writer.write("$L.expectStringNode().getValue()", varName);
        }
        return null;
    }

    @Override
    public Void structureShape(StructureShape shape) {
        writer.write("$L.fromNode($L)", TraitCodegenUtils.getDefaultName(shape), varName);
        return null;
    }

    @Override
    public Void timestampShape(TimestampShape shape) {
        if (shape.hasTrait(TimestampFormatTrait.ID)) {
            writeForTimestampFormat(shape);
        } else {
            writer.writeInline("$2T.parse($1L.expectStringNode().getValue())", varName, Instant.class);
        }
        return null;
    }

    @Override
    public Void unionShape(UnionShape shape) {
        writer.write("$L.fromNode($L)", TraitCodegenUtils.getDefaultName(shape), varName);
        return null;
    }

    @Override
    public Void blobShape(BlobShape shape) {
        throw new UnsupportedOperationException("Blob shapes not supported at this time.");
    }

    @Override
    public Void memberShape(MemberShape shape) {
        if (shape.hasTrait(IdRefTrait.ID)) {
            writer.write("$T.fromNode($L)", ShapeId.class, varName);
        } else if (shape.hasTrait(TimestampFormatTrait.ID)) {
            writeForTimestampFormat(shape);
        } else {
            model.expectShape(shape.getTarget()).accept(this);
        }
        return null;
    }

    @Override
    public Void enumShape(EnumShape shape) {
        writer.write("$L.fromNode($L)", TraitCodegenUtils.getDefaultName(shape), varName);
        return null;
    }

    @Override
    public Void intEnumShape(IntEnumShape shape) {
        writer.write("$L.expectNumberNode().getValue().intValue()", varName);
        return null;
    }

    private void writeForTimestampFormat(Shape shape) {
        switch (shape.expectTrait(TimestampFormatTrait.class).getFormat()) {
            case EPOCH_SECONDS:
                writer.writeInline("$2T.ofEpochSecond($1L.expectNumberNode().getValue().longValue())",
                        varName,
                        Instant.class);
                break;
            case HTTP_DATE:
                writer.writeInline("$2T.from($3T.RFC_1123_DATE_TIME.parse($1L.expectStringNode().getValue()))",
                        varName,
                        Instant.class,
                        DateTimeFormatter.class);
                break;
            default:
                // Fall through on default
                writer.writeInline("$2T.parse($1L.expectStringNode().getValue())", varName, Instant.class);
        }
    }
}
