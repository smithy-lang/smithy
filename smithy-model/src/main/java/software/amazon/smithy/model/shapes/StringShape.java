/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import java.util.Optional;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a {@code string} shape.
 */
public class StringShape extends SimpleShape implements ToSmithyBuilder<StringShape> {

    StringShape(Builder builder) {
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
        return visitor.stringShape(this);
    }

    @Override
    public Optional<StringShape> asStringShape() {
        return Optional.of(this);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.STRING;
    }

    /**
     * Builder used to create a {@link StringShape}.
     */
    public static class Builder extends AbstractShapeBuilder<Builder, StringShape> {
        @Override
        public StringShape build() {
            return new StringShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.STRING;
        }
    }
}
