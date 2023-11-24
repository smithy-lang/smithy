/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package software.amazon.smithy.model.traits;

import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class RecommendedTrait extends AbstractTrait implements ToSmithyBuilder<RecommendedTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#recommended");

    private final String reason;

    private RecommendedTrait(Builder builder) {
        super(ID, builder.sourceLocation);
        this.reason = builder.reason;
    }

    /**
     * Gets the reason it is recommended to set this member.
     *
     * @return returns the optional reason.
     */
    public Optional<String> getReason() {
        return Optional.ofNullable(reason);
    }

    @Override
    protected Node createNode() {
        return new ObjectNode(MapUtils.of(), getSourceLocation())
                .withOptionalMember("reason", getReason().map(Node::from));
    }

    @Override
    public RecommendedTrait.Builder toBuilder() {
        return builder().reason(reason).sourceLocation(getSourceLocation());
    }

    /**
     * @return Returns a new RecommendedTrait builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder used to create a RecommendedTrait.
     */
    public static final class Builder extends AbstractTraitBuilder<RecommendedTrait, Builder> {
        private String reason;

        public Builder reason(String reason) {
            this.reason = reason;
            return this;
        }

        @Override
        public RecommendedTrait build() {
            return new RecommendedTrait(this);
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public RecommendedTrait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value.getSourceLocation());
            value.expectObjectNode().getStringMember("reason", builder::reason);
            RecommendedTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }
}
