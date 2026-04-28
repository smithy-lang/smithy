/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Globally declares the type of a metadata key.
 *
 * <p>When this trait is applied to a shape, any metadata entry in the model
 * with the same key is validated against the targeted shape. A given
 * metadata key may only have its type declared once across the entire model.
 */
public final class MetadataTrait extends AbstractTrait implements ToSmithyBuilder<MetadataTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#metadata");

    private final String key;

    private MetadataTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.key = SmithyBuilder.requiredState("key", builder.key);
    }

    /**
     * @return Creates a builder for a {@link MetadataTrait}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return The metadata key that this trait defines a type for.
     */
    public String getKey() {
        return key;
    }

    @Override
    public Builder toBuilder() {
        return builder().sourceLocation(getSourceLocation()).key(key);
    }

    @Override
    protected Node createNode() {
        return Node.objectNodeBuilder()
                .sourceLocation(getSourceLocation())
                .withMember("key", key)
                .build();
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value.getSourceLocation());
            builder.key(value.expectObjectNode().expectStringMember("key").getValue());
            MetadataTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }

    public static final class Builder extends AbstractTraitBuilder<MetadataTrait, Builder> {
        private String key;

        private Builder() {}

        /**
         * Sets the metadata key that this trait defines a type for.
         *
         * @param key The metadata key. Required.
         * @return Returns the builder.
         */
        public Builder key(String key) {
            this.key = key;
            return this;
        }

        @Override
        public MetadataTrait build() {
            return new MetadataTrait(this);
        }
    }
}
