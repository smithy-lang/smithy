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

package software.amazon.smithy.aws.traits.protocols;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;

/**
 * An RPC-based protocol that sends JSON payloads.
 *
 * <p>Namespace names are included in shape IDs sent over the wire.
 */
public final class AwsJson1_0Trait extends AwsProtocolTrait {

    public static final ShapeId ID = ShapeId.from("aws.protocols#awsJson1_0");

    private AwsJson1_0Trait(Builder builder) {
        super(ID, builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AwsProtocolTrait.Builder<AwsJson1_0Trait, Builder> {
        private Builder() {}

        @Override
        public AwsJson1_0Trait build() {
            return new AwsJson1_0Trait(this);
        }
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public AwsJson1_0Trait createTrait(ShapeId target, Node value) {
            AwsJson1_0Trait result = builder().fromNode(value).build();
            result.setNodeCache(value);
            return result;
        }
    }
}
