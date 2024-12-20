/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import java.util.Optional;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a {@code short} shape.
 */
public final class ShortShape extends NumberShape implements ToSmithyBuilder<ShortShape> {

    private ShortShape(Builder builder) {
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
        return visitor.shortShape(this);
    }

    @Override
    public Optional<ShortShape> asShortShape() {
        return Optional.of(this);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.SHORT;
    }

    /**
     * Builder used to create a {@link ShortShape}.
     */
    public static final class Builder extends AbstractShapeBuilder<Builder, ShortShape> {
        @Override
        public ShortShape build() {
            return new ShortShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.SHORT;
        }
    }
}
