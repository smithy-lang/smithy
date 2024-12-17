/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import java.util.Optional;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a {@code list} shape.
 */
public class ListShape extends CollectionShape implements ToSmithyBuilder<ListShape> {

    ListShape(CollectionShape.Builder<? extends CollectionShape.Builder<?, ?>, ?> builder) {
        super(builder);
        validateMemberShapeIds();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return updateBuilder(builder()).member(getMember());
    }

    @Override
    public <R> R accept(ShapeVisitor<R> visitor) {
        return visitor.listShape(this);
    }

    @Override
    public Optional<ListShape> asListShape() {
        return Optional.of(this);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.LIST;
    }

    /**
     * Builder used to create a {@link ListShape}.
     */
    public static class Builder extends CollectionShape.Builder<Builder, ListShape> {
        @Override
        public ListShape build() {
            return new ListShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.LIST;
        }
    }
}
