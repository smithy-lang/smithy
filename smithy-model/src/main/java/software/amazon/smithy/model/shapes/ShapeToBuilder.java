/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

/**
 * Converts a shape to a {@link AbstractShapeBuilder}.
 */
final class ShapeToBuilder implements ShapeVisitor<AbstractShapeBuilder> {

    @SuppressWarnings("unchecked")
    public static <B extends AbstractShapeBuilder<B, S>, S extends Shape> B toBuilder(S shape) {
        return (B) shape.accept(new ShapeToBuilder());
    }

    @Override
    public AbstractShapeBuilder blobShape(BlobShape shape) {
        return shape.toBuilder();
    }

    @Override
    public AbstractShapeBuilder byteShape(ByteShape shape) {
        return shape.toBuilder();
    }

    @Override
    public AbstractShapeBuilder booleanShape(BooleanShape shape) {
        return shape.toBuilder();
    }

    @Override
    public AbstractShapeBuilder documentShape(DocumentShape shape) {
        return shape.toBuilder();
    }

    @Override
    public AbstractShapeBuilder doubleShape(DoubleShape shape) {
        return shape.toBuilder();
    }

    @Override
    public AbstractShapeBuilder listShape(ListShape shape) {
        return shape.toBuilder();
    }

    @Override
    public AbstractShapeBuilder mapShape(MapShape shape) {
        return shape.toBuilder();
    }

    @Override
    public AbstractShapeBuilder integerShape(IntegerShape shape) {
        return shape.toBuilder();
    }

    @Override
    public AbstractShapeBuilder intEnumShape(IntEnumShape shape) {
        return shape.toBuilder();
    }

    @Override
    public AbstractShapeBuilder longShape(LongShape shape) {
        return shape.toBuilder();
    }

    @Override
    public AbstractShapeBuilder floatShape(FloatShape shape) {
        return shape.toBuilder();
    }

    @Override
    public AbstractShapeBuilder bigIntegerShape(BigIntegerShape shape) {
        return shape.toBuilder();
    }

    @Override
    public AbstractShapeBuilder bigDecimalShape(BigDecimalShape shape) {
        return shape.toBuilder();
    }

    @Override
    public AbstractShapeBuilder operationShape(OperationShape shape) {
        return shape.toBuilder();
    }

    @Override
    public AbstractShapeBuilder resourceShape(ResourceShape shape) {
        return shape.toBuilder();
    }

    @Override
    public AbstractShapeBuilder shortShape(ShortShape shape) {
        return shape.toBuilder();
    }

    @Override
    public AbstractShapeBuilder serviceShape(ServiceShape shape) {
        return shape.toBuilder();
    }

    @Override
    public AbstractShapeBuilder stringShape(StringShape shape) {
        return shape.toBuilder();
    }

    @Override
    public AbstractShapeBuilder enumShape(EnumShape shape) {
        return shape.toBuilder();
    }

    @Override
    public AbstractShapeBuilder structureShape(StructureShape shape) {
        return shape.toBuilder();
    }

    @Override
    public AbstractShapeBuilder unionShape(UnionShape shape) {
        return shape.toBuilder();
    }

    @Override
    public AbstractShapeBuilder memberShape(MemberShape shape) {
        return shape.toBuilder();
    }

    @Override
    public AbstractShapeBuilder timestampShape(TimestampShape shape) {
        return shape.toBuilder();
    }
}
