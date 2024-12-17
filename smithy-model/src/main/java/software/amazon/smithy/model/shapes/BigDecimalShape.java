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
public final class BigDecimalShape extends NumberShape implements ToSmithyBuilder<BigDecimalShape> {

    private BigDecimalShape(Builder builder) {
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
        return visitor.bigDecimalShape(this);
    }

    @Override
    public Optional<BigDecimalShape> asBigDecimalShape() {
        return Optional.of(this);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.BIG_DECIMAL;
    }

    /**
     * Builder used to create a {@link BigDecimalShape}.
     */
    public static final class Builder extends AbstractShapeBuilder<Builder, BigDecimalShape> {
        @Override
        public BigDecimalShape build() {
            return new BigDecimalShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.BIG_DECIMAL;
        }
    }
}
