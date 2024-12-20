/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import java.util.Optional;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a {@code blob} shape.
 */
public final class BlobShape extends SimpleShape implements ToSmithyBuilder<BlobShape> {

    private BlobShape(Builder builder) {
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
        return visitor.blobShape(this);
    }

    @Override
    public Optional<BlobShape> asBlobShape() {
        return Optional.of(this);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.BLOB;
    }

    /**
     * Builder used to create a {@link BlobShape}.
     */
    public static final class Builder extends AbstractShapeBuilder<Builder, BlobShape> {
        @Override
        public BlobShape build() {
            return new BlobShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.BLOB;
        }
    }
}
