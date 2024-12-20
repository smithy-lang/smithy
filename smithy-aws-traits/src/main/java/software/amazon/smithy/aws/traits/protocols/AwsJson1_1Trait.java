/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.traits.protocols;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;

/**
 * An RPC-based protocol that sends JSON payloads.
 *
 * <p>Namespace names are stripped from shape IDs sent over the wire.
 */
public final class AwsJson1_1Trait extends AwsProtocolTrait {

    public static final ShapeId ID = ShapeId.from("aws.protocols#awsJson1_1");

    private AwsJson1_1Trait(Builder builder) {
        super(ID, builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends AwsProtocolTrait.Builder<AwsJson1_1Trait, Builder> {
        private Builder() {}

        @Override
        public AwsJson1_1Trait build() {
            return new AwsJson1_1Trait(this);
        }
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public AwsJson1_1Trait createTrait(ShapeId target, Node value) {
            AwsJson1_1Trait result = builder().fromNode(value).build();
            result.setNodeCache(value);
            return result;
        }
    }
}
