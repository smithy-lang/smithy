/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import software.amazon.smithy.model.SmithyBuilder;

/**
 * Shape visitor pattern.
 *
 * @param <R> Return type of the visitor.
 * @see Default
 * @see Shape#visitor()
 * @see Builder
 */
public interface ShapeVisitor<R> {

    R blobShape(BlobShape shape);

    R booleanShape(BooleanShape shape);

    R listShape(ListShape shape);

    R setShape(SetShape shape);

    R mapShape(MapShape shape);

    R byteShape(ByteShape shape);

    R shortShape(ShortShape shape);

    R integerShape(IntegerShape shape);

    R longShape(LongShape shape);

    R floatShape(FloatShape shape);

    R doubleShape(DoubleShape shape);

    R bigIntegerShape(BigIntegerShape shape);

    R bigDecimalShape(BigDecimalShape shape);

    R operationShape(OperationShape shape);

    R resourceShape(ResourceShape shape);

    R serviceShape(ServiceShape shape);

    R stringShape(StringShape shape);

    R structureShape(StructureShape shape);

    R unionShape(UnionShape shape);

    R memberShape(MemberShape shape);

    R timestampShape(TimestampShape shape);

    /**
     * Creates a {@link ShapeVisitor} used to dispatch to functions based
     * on a model type.
     *
     * @param <R> Return value from each case.
     */
    final class Builder<R> implements SmithyBuilder<ShapeVisitor<R>> {

        private Map<Class<? extends Shape>, Function<? extends Shape, R>> functions = new HashMap<>();
        private Function<Shape, R> orElse = shape -> {
            throw new IllegalStateException("Unexpected shape: " + shape.getClass().getCanonicalName());
        };

        Builder() {}

        @Override
        @SuppressWarnings("unchecked")
        public ShapeVisitor<R> build() {
            return new Default<R>() {
                protected R getDefault(Shape shape) {
                    Function<Shape, R> f = (Function<Shape, R>) functions.get(shape.getClass());
                    return f != null ? f.apply(shape) : orElse.apply(shape);
                }
            };
        }

        /**
         * Register a case for unhandled shapes and builds the Cases.
         *
         * @param f Function to use for unhandled visitor.
         * @return Returns the built {@link ShapeVisitor}
         */
        public ShapeVisitor<R> orElseGet(Function<Shape, R> f) {
            orElse = Objects.requireNonNull(f);
            return build();
        }

        /**
         * Register a case for unhandled shapes and builds the Cases.
         *
         * @param f Supplier to use for unhandled visitor.
         * @return Returns the built {@link ShapeVisitor}
         */
        public ShapeVisitor<R> orElseGet(Supplier<R> f) {
            return orElseGet(shape -> f.get());
        }

        /**
         * Return a value for all unhandled nodes.
         *
         * @param defaultValue Default value to return for unhandled visitor.
         * @return Returns the built {@link ShapeVisitor}
         */
        public ShapeVisitor<R> orElse(R defaultValue) {
            return orElseGet(shape -> defaultValue);
        }

        /**
         * Register a case for unhandled shapes that throws an exception.
         *
         * @param e Exception to throw if an unhandled case is encountered.
         * @return Returns the built {@link ShapeVisitor}
         */
        public ShapeVisitor<R> orElseThrow(RuntimeException e) {
            return orElseGet(shape -> {
                throw e;
            });
        }

        /**
         * Invoked when a specific shape type is encountered.
         *
         * @param type Shape type to handle.
         * @param f Function that accepts the shape and returns R.
         * @param <T> The shape type being handled.
         * @return Returns the visitor builder.
         */
        public <T extends Shape> Builder<R> when(Class<T> type, Function<T, R> f) {
            functions.put(type, Objects.requireNonNull(f));
            return this;
        }
    }

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
        public R setShape(SetShape shape) {
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
}
