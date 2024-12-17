/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import java.util.Optional;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a {@code boolean} shape.
 */
public final class BooleanShape extends SimpleShape implements ToSmithyBuilder<BooleanShape> {

    private BooleanShape(Builder builder) {
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
        return visitor.booleanShape(this);
    }

    @Override
    public Optional<BooleanShape> asBooleanShape() {
        return Optional.of(this);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.BOOLEAN;
    }

    /**
     * Builder used to create a {@link BooleanShape}.
     */
    public static final class Builder extends AbstractShapeBuilder<Builder, BooleanShape> {
        @Override
        public BooleanShape build() {
            return new BooleanShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.BOOLEAN;
        }
    }
}
