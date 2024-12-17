/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.tagging;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.TraitService;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Trait annotating a service shape as having taggable resources. Should also contain consistent tagging operations.
 */
public final class TagEnabledTrait extends AbstractTrait implements ToSmithyBuilder<TagEnabledTrait> {
    public static final ShapeId ID = ShapeId.from("aws.api#tagEnabled");

    private final boolean disableDefaultOperations;

    public TagEnabledTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        disableDefaultOperations = builder.disableDefaultOperations;
    }

    @Override
    protected Node createNode() {
        return new ObjectNode(MapUtils.of(), getSourceLocation())
                .withMember("disableDefaultOperations", getDisableDefaultOperations());
    }

    public boolean getDisableDefaultOperations() {
        return disableDefaultOperations;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder().disableDefaultOperations(disableDefaultOperations);
    }

    public static final class Builder extends AbstractTraitBuilder<TagEnabledTrait, Builder> {
        private Boolean disableDefaultOperations = false;

        public Builder disableDefaultOperations(Boolean disableDefaultOperations) {
            this.disableDefaultOperations = disableDefaultOperations;
            return this;
        }

        @Override
        public TagEnabledTrait build() {
            return new TagEnabledTrait(this);
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public TagEnabledTrait createTrait(ShapeId target, Node value) {
            ObjectNode objectNode = value.expectObjectNode();
            Boolean name = objectNode.getBooleanMemberOrDefault("disableDefaultOperations", false);
            TagEnabledTrait result = builder().sourceLocation(value).disableDefaultOperations(name).build();
            result.setNodeCache(value);
            return result;
        }
    }
}
