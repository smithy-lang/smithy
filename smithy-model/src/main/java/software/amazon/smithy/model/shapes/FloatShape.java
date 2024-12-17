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
public final class FloatShape extends NumberShape implements ToSmithyBuilder<FloatShape> {

    private FloatShape(Builder builder) {
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
        return visitor.floatShape(this);
    }

    @Override
    public Optional<FloatShape> asFloatShape() {
        return Optional.of(this);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.FLOAT;
    }

    /**
     * Builder used to create a {@link FloatShape}.
     */
    public static final class Builder extends AbstractShapeBuilder<Builder, FloatShape> {
        @Override
        public FloatShape build() {
            return new FloatShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.FLOAT;
        }
    }
}
