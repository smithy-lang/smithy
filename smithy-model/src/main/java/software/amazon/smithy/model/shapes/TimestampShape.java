/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import java.util.Optional;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a {@code timestamp} shape.
 */
public final class TimestampShape extends SimpleShape implements ToSmithyBuilder<TimestampShape> {
    private TimestampShape(Builder builder) {
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
        return visitor.timestampShape(this);
    }

    @Override
    public Optional<TimestampShape> asTimestampShape() {
        return Optional.of(this);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.TIMESTAMP;
    }

    /**
     * Builder used to create a {@link TimestampShape}.
     */
    public static final class Builder extends AbstractShapeBuilder<Builder, TimestampShape> {
        @Override
        public TimestampShape build() {
            return new TimestampShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.TIMESTAMP;
        }
    }
}
