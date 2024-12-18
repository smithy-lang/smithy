/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.traitcodegen.generators;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import software.amazon.smithy.model.Model;
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
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.IdRefTrait;
import software.amazon.smithy.model.traits.TimestampFormatTrait;
import software.amazon.smithy.traitcodegen.TraitCodegenUtils;
import software.amazon.smithy.traitcodegen.writer.TraitCodegenWriter;

/**
 * Determines how to map a node to a shape.
 */
final class FromNodeMapperVisitor extends ShapeVisitor.DataShapeVisitor<Void> {

    private final TraitCodegenWriter writer;
    private final Model model;
    private final String varName;

    FromNodeMapperVisitor(TraitCodegenWriter writer, Model model, String varName) {
        this.writer = writer;
        this.model = model;
        this.varName = varName;
    }

    @Override
    public Void booleanShape(BooleanShape shape) {
        writer.write("BooleanMember($1S, builder::$1L)", varName);
        return null;
    }

    @Override
    public Void listShape(ListShape shape) {
        writer.write("$L.expectArrayNode()", varName);
        writer.indent();
        writer.writeWithNoFormatting(".getElements().stream()");
        writer.write(".map(n -> $C)",
                (Runnable) () -> shape.getMember().accept(new FromNodeMapperVisitor(writer, model, "n")));
        writer.writeWithNoFormatting(".forEach(builder::addValues);");
        writer.dedent();
        return null;
    }

    @Override
    public Void mapShape(MapShape shape) {
        writer.openBlock("$L.expectObjectNode().getMembers().forEach((k, v) -> {",
                "});",
                varName,
                () -> writer.write("builder.putValues($C, $C);",
                        (Runnable) () -> shape.getKey().accept(new FromNodeMapperVisitor(writer, model, "k")),
                        (Runnable) () -> shape.getValue().accept(new FromNodeMapperVisitor(writer, model, "v"))));
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
        if (shape.hasTrait(IdRefTrait.class)) {
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
        if (shape.hasTrait(TimestampFormatTrait.class)) {
            switch (shape.expectTrait(TimestampFormatTrait.class).getFormat()) {
                case EPOCH_SECONDS:
                    writer.writeInline("$2T.ofEpochSecond($1L.expectNumberNode().getValue().longValue())",
                            varName,
                            Instant.class);
                    return null;
                case HTTP_DATE:
                    writer.writeInline("$2T.from($3T.RFC_1123_DATE_TIME.parse($1L.expectStringNode().getValue()))",
                            varName,
                            Instant.class,
                            DateTimeFormatter.class);
                    return null;
                default:
                    // Fall through on default
                    break;
            }
        }
        writer.writeInline("$2T.parse($1L.expectStringNode().getValue())", varName, Instant.class);
        return null;
    }

    @Override
    public Void unionShape(UnionShape shape) {
        throw new UnsupportedOperationException("Union shapes not supported at this time.");
    }

    @Override
    public Void blobShape(BlobShape shape) {
        throw new UnsupportedOperationException("Blob shapes not supported at this time.");
    }

    @Override
    public Void memberShape(MemberShape shape) {
        if (shape.hasTrait(IdRefTrait.class)) {
            writer.write("$T.fromNode($L)", ShapeId.class, varName);
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
}
