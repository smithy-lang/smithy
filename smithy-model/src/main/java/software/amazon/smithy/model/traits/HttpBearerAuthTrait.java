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

import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * An auth scheme trait uses HTTP bearer auth.
 */
public final class HttpBearerAuthTrait extends AbstractTrait implements ToSmithyBuilder<HttpBearerAuthTrait>  {

    public static final ShapeId ID = ShapeId.from("smithy.api#httpBearerAuth");
    public static final String BEARER_FORMAT = "bearerFormat";

    private final String bearerFormat;

    private HttpBearerAuthTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        bearerFormat = builder.bearerFormat;
    }

    public HttpBearerAuthTrait() {
        this(Node.objectNode());
    }

    public HttpBearerAuthTrait(ObjectNode node) {
        super(ID, node);
        bearerFormat = node.getStringMember(BEARER_FORMAT).map(StringNode::getValue).orElse(null);
    }

    /**
     * Gets the bearer format.
     *
     * @return returns the optional bearer format.
     */
    public Optional<String> getBearerFormat() {
        return Optional.ofNullable(bearerFormat);
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .bearerFormat(bearerFormat);
    }

    @Override
    protected Node createNode() {
        return Node.objectNodeBuilder()
                .withOptionalMember(BEARER_FORMAT, getBearerFormat().map(Node::from))
                .build();
    }

    /**
     * @return Returns a new HttpBearerAuthTrait builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds a  used to create a HttpBearerAuthTrait.
     */
    public static final class Builder extends AbstractTraitBuilder<HttpBearerAuthTrait, Builder> {
        private String bearerFormat;

        private Builder() {}

        @Override
        public HttpBearerAuthTrait build() {
            return new HttpBearerAuthTrait(this);
        }

        public Builder bearerFormat(String bearerFormat) {
            this.bearerFormat = bearerFormat;
            return this;
        }
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value.getSourceLocation());
            ObjectNode objectNode = value.expectObjectNode();
            objectNode.getStringMember(BEARER_FORMAT)
                    .map(StringNode::getValue)
                    .ifPresent(builder::bearerFormat);
            return builder.build();
        }
    }
}
