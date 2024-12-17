/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.shapes;

import java.util.Optional;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Represents a {@code document} shape.
 */
public final class DocumentShape extends SimpleShape implements ToSmithyBuilder<DocumentShape> {

    private DocumentShape(Builder builder) {
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
        return visitor.documentShape(this);
    }

    @Override
    public Optional<DocumentShape> asDocumentShape() {
        return Optional.of(this);
    }

    @Override
    public ShapeType getType() {
        return ShapeType.DOCUMENT;
    }

    /**
     * Builder used to create a {@link DocumentShape}.
     */
    public static final class Builder extends AbstractShapeBuilder<Builder, DocumentShape> {
        @Override
        public DocumentShape build() {
            return new DocumentShape(this);
        }

        @Override
        public ShapeType getShapeType() {
            return ShapeType.DOCUMENT;
        }
    }
}
