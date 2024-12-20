/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import java.util.Optional;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a {@code integer} shape.
 */
public final class BigIntegerShape extends NumberShape implements ToSmithyBuilder<BigIntegerShape> {

    private BigIntegerShape(Builder builder) {
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
        return visitor.bigIntegerShape(this);
    }

    @Override
    public Optional<BigIntegerShape> asBigIntegerShape() {
        return Optional.of(this);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.BIG_INTEGER;
    }

    /**
     * Builder used to create a {@link BigIntegerShape}.
     */
    public static final class Builder extends AbstractShapeBuilder<Builder, BigIntegerShape> {
        @Override
        public BigIntegerShape build() {
            return new BigIntegerShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.BIG_INTEGER;
        }
    }
}
