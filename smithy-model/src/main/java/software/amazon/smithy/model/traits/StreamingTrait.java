/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Indicates that the the data stored in the shape is very large and should
 * not be stored in memory, or that the size of the data stored in the
 * shape is unknown at the start of a request.
 *
 * TODO: Ensure that there is only one streaming blob per operation in/out.
 */
public final class StreamingTrait extends AbstractTrait implements ToSmithyBuilder<StreamingTrait> {
    public static final String NAME = "smithy.api#streaming";
    private static final String REQUIRES_LENGTH = "requiresLength";

    private final boolean requiresLength;

    private StreamingTrait(Builder builder) {
        super(NAME, builder.getSourceLocation());
        requiresLength = builder.requiresLength;
    }

    /**
     * Creates a builder for a streaming trait.
     *
     * @return Returns the created builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return Returns true if the stream requires a known length.
     */
    public boolean getRequiresLength() {
        return requiresLength;
    }

    @Override
    public Builder toBuilder() {
        return builder().requiresLength(requiresLength);
    }

    @Override
    protected Node createNode() {
        return requiresLength ? Node.objectNode().withMember(REQUIRES_LENGTH, true) : Node.objectNode();
    }

    public static final class Provider implements TraitService {
        @Override
        public String getTraitName() {
            return NAME;
        }

        @Override
        public StreamingTrait createTrait(ShapeId target, Node value) {
            ObjectNode node = value.expectObjectNode();
            return builder().requiresLength(node.getBooleanMemberOrDefault(REQUIRES_LENGTH)).build();
        }
    }

    /**
     * Builds a {@link StreamingTrait} trait.
     */
    public static final class Builder extends AbstractTraitBuilder<StreamingTrait, Builder> {
        private boolean requiresLength;

        private Builder() {}

        @Override
        public StreamingTrait build() {
            return new StreamingTrait(this);
        }

        /**
         * Indicates if the length of the stream must be known.
         *
         * @param requiresLength Set to true to require a known length.
         * @return Returns the builder.
         */
        public Builder requiresLength(boolean requiresLength) {
            this.requiresLength = requiresLength;
            return this;
        }
    }
}
