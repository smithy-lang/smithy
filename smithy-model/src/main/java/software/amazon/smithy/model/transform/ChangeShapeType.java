/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.transform;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
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
import software.amazon.smithy.model.shapes.MemberShape;
import software.amazon.smithy.model.shapes.SetShape;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.shapes.ShapeType;
import software.amazon.smithy.model.shapes.ShapeVisitor;
import software.amazon.smithy.model.shapes.ShortShape;
import software.amazon.smithy.model.shapes.StringShape;
import software.amazon.smithy.model.shapes.StructureShape;
import software.amazon.smithy.model.shapes.TimestampShape;
import software.amazon.smithy.model.shapes.UnionShape;
import software.amazon.smithy.model.traits.EnumTrait;
import software.amazon.smithy.model.traits.synthetic.SyntheticEnumTrait;

final class ChangeShapeType {

    private final Map<ShapeId, ShapeType> shapeToType;
    private final boolean synthesizeEnumNames;

    ChangeShapeType(Map<ShapeId, ShapeType> shapeToType, boolean synthesizeEnumNames) {
        this.shapeToType = shapeToType;
        this.synthesizeEnumNames = synthesizeEnumNames;
    }

    ChangeShapeType(Map<ShapeId, ShapeType> shapeToType) {
        this(shapeToType, false);
    }

    static ChangeShapeType upgradeEnums(Model model, boolean synthesizeEnumNames) {
        Map<ShapeId, ShapeType> toUpdate = new HashMap<>();
        for (StringShape shape : model.getStringShapesWithTrait(EnumTrait.class)) {
            if (EnumShape.canConvertToEnum(shape, synthesizeEnumNames)) {
                toUpdate.put(shape.getId(), ShapeType.ENUM);
            }
        }
        return new ChangeShapeType(toUpdate, synthesizeEnumNames);
    }

    static ChangeShapeType downgradeEnums(Model model) {
        Map<ShapeId, ShapeType> toUpdate = new HashMap<>();
        for (EnumShape shape : model.getEnumShapes()) {
            toUpdate.put(shape.getId(), ShapeType.STRING);
        }
        for (IntEnumShape shape : model.getIntEnumShapes()) {
            toUpdate.put(shape.getId(), ShapeType.INTEGER);
        }
        return new ChangeShapeType(toUpdate);
    }

    Model transform(ModelTransformer transformer, Model model) {
        return transformer.mapShapes(model, shape -> {
            if (shapeToType.containsKey(shape.getId())) {
                ShapeType targetType = shapeToType.get(shape.getId());
                return targetType == shape.getType()
                        ? shape
                        : shape.accept(new Retype(targetType, synthesizeEnumNames));
            } else {
                return shape;
            }
        });
    }

    private static final class Retype extends ShapeVisitor.Default<Shape> {
        private final ShapeType to;
        private final boolean synthesizeEnumNames;

        Retype(ShapeType to, boolean synthesizeEnumNames) {
            this.to = to;
            this.synthesizeEnumNames = synthesizeEnumNames;
        }

        @Override
        protected Shape getDefault(Shape shape) {
            throw invalidType(shape, to, shape.getType() + " cannot be retyped.");
        }

        @Override
        public Shape blobShape(BlobShape shape) {
            return copyToSimpleShape(to, shape);
        }

        @Override
        public Shape booleanShape(BooleanShape shape) {
            return copyToSimpleShape(to, shape);
        }

        @Override
        public Shape byteShape(ByteShape shape) {
            return copyToSimpleShape(to, shape);
        }

        @Override
        public Shape shortShape(ShortShape shape) {
            return copyToSimpleShape(to, shape);
        }

        @Override
        public Shape integerShape(IntegerShape shape) {
            return copyToSimpleShape(to, shape);
        }

        @Override
        public Shape intEnumShape(IntEnumShape shape) {
            if (to.getCategory() != ShapeType.Category.SIMPLE) {
                throw invalidType(shape, to, "Enum types can only be converted to simple types.");
            }

            AbstractShapeBuilder<?, ?> shapeBuilder = to.createBuilderForType();
            copySharedParts(shape, shapeBuilder);
            return shapeBuilder.build();
        }

        @Override
        public Shape longShape(LongShape shape) {
            return copyToSimpleShape(to, shape);
        }

        @Override
        public Shape floatShape(FloatShape shape) {
            return copyToSimpleShape(to, shape);
        }

        @Override
        public Shape doubleShape(DoubleShape shape) {
            return copyToSimpleShape(to, shape);
        }

        @Override
        public Shape documentShape(DocumentShape shape) {
            return copyToSimpleShape(to, shape);
        }

        @Override
        public Shape bigIntegerShape(BigIntegerShape shape) {
            return copyToSimpleShape(to, shape);
        }

        @Override
        public Shape bigDecimalShape(BigDecimalShape shape) {
            return copyToSimpleShape(to, shape);
        }

        @Override
        public Shape stringShape(StringShape shape) {
            if (to == ShapeType.ENUM) {
                Optional<EnumShape> enumShape = EnumShape.fromStringShape(shape, synthesizeEnumNames);
                if (enumShape.isPresent()) {
                    return enumShape.get();
                }
                throw invalidType(shape,
                        to,
                        "Strings can only be converted to enums if they have an enum "
                                + "trait where each enum definition has a name.");
            }
            return copyToSimpleShape(to, shape);
        }

        @Override
        public Shape enumShape(EnumShape shape) {
            if (to.getCategory() != ShapeType.Category.SIMPLE) {
                throw invalidType(shape, to, "Enum types can only be converted to simple types.");
            }

            AbstractShapeBuilder<?, ?> shapeBuilder = to.createBuilderForType();
            copySharedParts(shape, shapeBuilder);
            shapeBuilder.removeTrait(SyntheticEnumTrait.ID);

            if (to == ShapeType.STRING) {
                EnumTrait.Builder traitBuilder = EnumTrait.builder();
                shape.expectTrait(SyntheticEnumTrait.class).getValues().forEach(traitBuilder::addEnum);
                shapeBuilder.addTrait(traitBuilder.build());
            }

            return shapeBuilder.build();
        }

        @Override
        public Shape timestampShape(TimestampShape shape) {
            return copyToSimpleShape(to, shape);
        }

        @Override
        public Shape listShape(ListShape shape) {
            if (to != ShapeType.SET) {
                throw invalidType(shape, to, "Lists can only be converted to sets.");
            }
            SetShape.Builder builder = SetShape.builder();
            copySharedPartsAndMembers(shape, builder);
            return builder.build();
        }

        @Override
        public Shape setShape(SetShape shape) {
            if (to != ShapeType.LIST) {
                throw invalidType(shape, to, "Sets can only be converted to lists.");
            }
            ListShape.Builder builder = ListShape.builder();
            copySharedPartsAndMembers(shape, builder);
            return builder.build();
        }

        @Override
        public Shape structureShape(StructureShape shape) {
            if (to != ShapeType.UNION) {
                throw invalidType(shape, to, "Structures can only be converted to unions.");
            }
            UnionShape.Builder builder = UnionShape.builder();
            copySharedPartsAndMembers(shape, builder);
            return builder.build();
        }

        @Override
        public Shape unionShape(UnionShape shape) {
            if (to != ShapeType.STRUCTURE) {
                throw invalidType(shape, to, "Unions can only be converted to structures.");
            }
            StructureShape.Builder builder = StructureShape.builder();
            copySharedPartsAndMembers(shape, builder);
            return builder.build();
        }

        private void copySharedPartsAndMembers(Shape source, AbstractShapeBuilder<?, ?> builder) {
            copySharedParts(source, builder);
            for (MemberShape member : source.members()) {
                builder.addMember(member);
            }
        }

        private void copySharedParts(Shape source, AbstractShapeBuilder<?, ?> builder) {
            builder.traits(source.getAllTraits().values());
            builder.id(source.getId());
            builder.source(source.getSourceLocation());
        }

        private Shape copyToSimpleShape(ShapeType to, Shape shape) {
            if (to.getCategory() != ShapeType.Category.SIMPLE) {
                throw invalidType(shape, to, "Simple types can only be converted to other simple types.");
            } else if (to == ShapeType.ENUM || to == ShapeType.INT_ENUM) {
                throw invalidType(shape, to, "This simple type cannot be converted to an enum type.");
            }

            AbstractShapeBuilder<?, ?> shapeBuilder = to.createBuilderForType();
            copySharedPartsAndMembers(shape, shapeBuilder);
            return shapeBuilder.build();
        }

        private IllegalArgumentException invalidType(Shape shape, ShapeType to, String message) {
            return new IllegalArgumentException("Cannot convert " + shape + " to " + to + ". " + message);
        }
    }
}
