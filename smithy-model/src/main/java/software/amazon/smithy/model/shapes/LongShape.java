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
public final class LongShape extends NumberShape implements ToSmithyBuilder<LongShape> {

    private LongShape(Builder builder) {
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
        return visitor.longShape(this);
    }

    @Override
    public Optional<LongShape> asLongShape() {
        return Optional.of(this);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.LONG;
    }

    /**
     * Builder used to create a {@link LongShape}.
     */
    public static final class Builder extends AbstractShapeBuilder<Builder, LongShape> {
        @Override
        public LongShape build() {
            return new LongShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.LONG;
        }
    }
}
