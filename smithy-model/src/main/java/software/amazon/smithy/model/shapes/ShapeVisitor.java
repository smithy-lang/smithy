/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.shapes;

/**
 * Shape visitor pattern.
 *
 * @param <R> Return type of the visitor.
 * @see Default
 */
public interface ShapeVisitor<R> {

    R blobShape(BlobShape shape);

    R booleanShape(BooleanShape shape);

    R listShape(ListShape shape);

    @Deprecated
    default R setShape(SetShape shape) {
        return listShape(shape);
    }

    R mapShape(MapShape shape);

    R byteShape(ByteShape shape);

    R shortShape(ShortShape shape);

    R integerShape(IntegerShape shape);

    default R intEnumShape(IntEnumShape shape) {
        return integerShape(shape);
    }

    R longShape(LongShape shape);

    R floatShape(FloatShape shape);

    R documentShape(DocumentShape shape);

    R doubleShape(DoubleShape shape);

    R bigIntegerShape(BigIntegerShape shape);

    R bigDecimalShape(BigDecimalShape shape);

    R operationShape(OperationShape shape);

    R resourceShape(ResourceShape shape);

    R serviceShape(ServiceShape shape);

    R stringShape(StringShape shape);

    default R enumShape(EnumShape shape) {
        return stringShape(shape);
    }

    R structureShape(StructureShape shape);

    R unionShape(UnionShape shape);

    R memberShape(MemberShape shape);

    R timestampShape(TimestampShape shape);

    /**
     * Creates {@link ShapeVisitor} that return a value when necessary
     * when visiting shapes.
     *
     * @param <R> Return type.
     */
    abstract class Default<R> implements ShapeVisitor<R> {

        /**
         * Returns a value for any unhandled shape.
         *
         * @param shape Shape that is being visited.
         * @return Return value.
         */
        protected abstract R getDefault(Shape shape);

        @Override
        public R blobShape(BlobShape shape) {
            return getDefault(shape);
        }

        @Override
        public R booleanShape(BooleanShape shape) {
            return getDefault(shape);
        }

        @Override
        public R listShape(ListShape shape) {
            return getDefault(shape);
        }

        @Override
        public R byteShape(ByteShape shape) {
            return getDefault(shape);
        }

        @Override
        public R shortShape(ShortShape shape) {
            return getDefault(shape);
        }

        @Override
        public R integerShape(IntegerShape shape) {
            return getDefault(shape);
        }

        @Override
        public R longShape(LongShape shape) {
            return getDefault(shape);
        }

        @Override
        public R floatShape(FloatShape shape) {
            return getDefault(shape);
        }

        @Override
        public R documentShape(DocumentShape shape) {
            return getDefault(shape);
        }

        @Override
        public R doubleShape(DoubleShape shape) {
            return getDefault(shape);
        }

        @Override
        public R bigIntegerShape(BigIntegerShape shape) {
            return getDefault(shape);
        }

        @Override
        public R bigDecimalShape(BigDecimalShape shape) {
            return getDefault(shape);
        }

        @Override
        public R mapShape(MapShape shape) {
            return getDefault(shape);
        }

        @Override
        public R operationShape(OperationShape shape) {
            return getDefault(shape);
        }

        @Override
        public R resourceShape(ResourceShape shape) {
            return getDefault(shape);
        }

        @Override
        public R serviceShape(ServiceShape shape) {
            return getDefault(shape);
        }

        @Override
        public R stringShape(StringShape shape) {
            return getDefault(shape);
        }

        @Override
        public R structureShape(StructureShape shape) {
            return getDefault(shape);
        }

        @Override
        public R unionShape(UnionShape shape) {
            return getDefault(shape);
        }

        @Override
        public R memberShape(MemberShape shape) {
            return getDefault(shape);
        }

        @Override
        public R timestampShape(TimestampShape shape) {
            return getDefault(shape);
        }
    }

    /**
     * Creates {@link ShapeVisitor} that only requires implementation of
     * all data shape branches, but does not support service shapes.
     *
     * @param <R> Return type.
     */
    abstract class DataShapeVisitor<R> implements ShapeVisitor<R> {
        @Override
        public R operationShape(OperationShape shape) {
            throw new IllegalArgumentException("DataShapeVisitor cannot be use to visit "
                    + "Operation Shapes. Attempted to visit: " + shape);
        }

        @Override
        public R resourceShape(ResourceShape shape) {
            throw new IllegalArgumentException("DataShapeVisitor cannot be use to visit "
                    + "Resource Shapes. Attempted to visit: " + shape);
        }

        @Override
        public R serviceShape(ServiceShape shape) {
            throw new IllegalArgumentException("DataShapeVisitor cannot be use to visit "
                + "Service Shapes. Attempted to visit: " + shape);
        }
    }
}
