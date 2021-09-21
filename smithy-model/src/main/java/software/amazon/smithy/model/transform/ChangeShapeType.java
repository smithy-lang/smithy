/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.transform;

import java.util.Map;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.shapes.AbstractShapeBuilder;
import software.amazon.smithy.model.shapes.BigDecimalShape;
import software.amazon.smithy.model.shapes.BigIntegerShape;
import software.amazon.smithy.model.shapes.BlobShape;
import software.amazon.smithy.model.shapes.BooleanShape;
import software.amazon.smithy.model.shapes.ByteShape;
import software.amazon.smithy.model.shapes.DocumentShape;
import software.amazon.smithy.model.shapes.DoubleShape;
import software.amazon.smithy.model.shapes.FloatShape;
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

final class ChangeShapeType {

    private final Map<ShapeId, ShapeType> shapeToType;

    ChangeShapeType(Map<ShapeId, ShapeType> shapeToType) {
        this.shapeToType = shapeToType;
    }

    Model transform(ModelTransformer transformer, Model model) {
        return transformer.mapShapes(model, shape -> {
            if (shapeToType.containsKey(shape.getId())) {
                return shape.accept(new Retype(shapeToType.get(shape.getId())));
            } else {
                return shape;
            }
        });
    }

    private static final class Retype extends ShapeVisitor.Default<Shape> {
        private final ShapeType to;

        Retype(ShapeType to) {
            this.to = to;
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
            return copyToSimpleShape(to, shape);
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
            copySharedPartsToShape(shape, builder);
            return builder.build();
        }

        @Override
        public Shape setShape(SetShape shape) {
            if (to != ShapeType.LIST) {
                throw invalidType(shape, to, "Sets can only be converted to lists.");
            }
            ListShape.Builder builder = ListShape.builder();
            copySharedPartsToShape(shape, builder);
            return builder.build();
        }

        @Override
        public Shape structureShape(StructureShape shape) {
            if (to != ShapeType.UNION) {
                throw invalidType(shape, to, "Structures can only be converted to unions.");
            }
            UnionShape.Builder builder = UnionShape.builder();
            copySharedPartsToShape(shape, builder);
            return builder.build();
        }

        @Override
        public Shape unionShape(UnionShape shape) {
            if (to != ShapeType.STRUCTURE) {
                throw invalidType(shape, to, "Unions can only be converted to structures.");
            }
            StructureShape.Builder builder = StructureShape.builder();
            copySharedPartsToShape(shape, builder);
            return builder.build();
        }

        private void copySharedPartsToShape(Shape source, AbstractShapeBuilder<?, ?> builder) {
            builder.traits(source.getAllTraits().values());
            builder.id(source.getId());
            builder.source(source.getSourceLocation());

            for (MemberShape member : source.members()) {
                builder.addMember(member);
            }
        }

        private Shape copyToSimpleShape(ShapeType to, Shape shape) {
            if (to.getCategory() != ShapeType.Category.SIMPLE) {
                throw invalidType(shape, to, "Simple types can only be converted to other simple types.");
            }

            AbstractShapeBuilder<?, ?> shapeBuilder = to.createBuilderForType();
            copySharedPartsToShape(shape, shapeBuilder);
            return shapeBuilder.build();
        }

        private IllegalArgumentException invalidType(Shape shape, ShapeType to, String message) {
            return new IllegalArgumentException("Cannot convert " + shape + " to " + to + ". " + message);
        }
    }
}
