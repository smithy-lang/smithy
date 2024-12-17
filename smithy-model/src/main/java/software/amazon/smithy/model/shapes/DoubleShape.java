/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import java.util.Optional;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a {@code double} shape.
 */
public final class DoubleShape extends NumberShape implements ToSmithyBuilder<DoubleShape> {

    private DoubleShape(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return updateBuilder(builder());
    }

    @Override
    public <R> R accept(ShapeVisitor<R> visitor) {
        return visitor.doubleShape(this);
    }

    @Override
    public Optional<DoubleShape> asDoubleShape() {
        return Optional.of(this);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.DOUBLE;
    }

    /**
     * Builder used to create a {@link DoubleShape}.
     */
    public static final class Builder extends AbstractShapeBuilder<Builder, DoubleShape> {
        @Override
        public DoubleShape build() {
            return new DoubleShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.DOUBLE;
        }
    }
}
