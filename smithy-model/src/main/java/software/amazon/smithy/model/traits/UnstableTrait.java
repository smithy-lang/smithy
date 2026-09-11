/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Indicates that a shape is unstable and could change in the future.
 *
 * <p>An optional feature id associates the shape with a preview feature defined by the enclosing service's
 * unstableFeatures trait.
 *
 * @see UnstableFeaturesTrait
 */
public final class UnstableTrait extends AbstractTrait implements ToSmithyBuilder<UnstableTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#unstable");

    private final String featureId;

    public UnstableTrait(ObjectNode node) {
        this(nodeToBuilder(node));
    }

    private UnstableTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.featureId = builder.featureId;
    }

    public UnstableTrait() {
        this(builder());
    }

    private static Builder nodeToBuilder(ObjectNode node) {
        Builder builder = builder().sourceLocation(node);
        node.getStringMember("featureId", builder::featureId);
        return builder;
    }

    /**
     * Gets the id of the unstable feature this shape belongs to, if any.
     *
     * @return Returns the optional feature id.
     */
    public Optional<String> getFeatureId() {
        return Optional.ofNullable(featureId);
    }

    @Override
    protected Node createNode() {
        return Node.objectNodeBuilder()
                .sourceLocation(getSourceLocation())
                .withOptionalMember("featureId", getFeatureId().map(Node::from))
                .build();
    }

    @Override
    public Builder toBuilder() {
        return new Builder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AbstractTraitBuilder<UnstableTrait, Builder> {
        private String featureId;

        private Builder() {}

        private Builder(UnstableTrait trait) {
            sourceLocation(trait.getSourceLocation());
            this.featureId = trait.featureId;
        }

        @Override
        public UnstableTrait build() {
            return new UnstableTrait(this);
        }

        public Builder featureId(String featureId) {
            this.featureId = featureId;
            return this;
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public UnstableTrait createTrait(ShapeId target, Node value) {
            UnstableTrait result = nodeToBuilder(value.expectObjectNode()).build();
            result.setNodeCache(value);
            return result;
        }
    }
}
