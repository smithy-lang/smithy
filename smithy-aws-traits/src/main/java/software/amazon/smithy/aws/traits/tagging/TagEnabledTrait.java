/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
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
