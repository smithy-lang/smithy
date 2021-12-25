/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;

/**
 * A RESTful protocol that sends XML in structured payloads.
 */
public final class RestXmlTrait extends AwsProtocolTrait {

    public static final ShapeId ID = ShapeId.from("aws.protocols#restXml");

    private final boolean noErrorWrapping;

    private RestXmlTrait(Builder builder) {
        super(ID, builder);
        this.noErrorWrapping = builder.noErrorWrapping;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return Returns the noErrorWrapping setting.
     */
    public boolean isNoErrorWrapping() {
        return noErrorWrapping;
    }

    @Override
    protected Node createNode() {
        ObjectNode node = super.createNode().expectObjectNode();

        if (isNoErrorWrapping()) {
            node = node.withMember("noErrorWrapping", Node.from(noErrorWrapping));
        }

        return node;
    }

    public static final class Builder extends AwsProtocolTrait.Builder<RestXmlTrait, Builder> {
        private boolean noErrorWrapping = false;

        private Builder() {}

        public Builder noErrorWrapping(boolean noErrorWrapping) {
            this.noErrorWrapping = noErrorWrapping;
            return this;
        }

        @Override
        public Builder fromNode(Node node) {
            Builder builder = super.fromNode(node);

            ObjectNode objectNode = node.expectObjectNode();
            builder.noErrorWrapping(objectNode.getBooleanMemberOrDefault("noErrorWrapping"));

            return builder;
        }

        @Override
        public RestXmlTrait build() {
            return new RestXmlTrait(this);
        }
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public RestXmlTrait createTrait(ShapeId target, Node value) {
            RestXmlTrait result = builder().fromNode(value).build();
            result.setNodeCache(value);
            return result;
        }
    }
}
