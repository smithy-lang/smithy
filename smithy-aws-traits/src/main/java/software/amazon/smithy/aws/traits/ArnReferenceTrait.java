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

package software.amazon.smithy.aws.traits;

import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Indicates that a string shape contains an ARN.
 */
public final class ArnReferenceTrait extends AbstractTrait implements ToSmithyBuilder<ArnReferenceTrait> {
    public static final ShapeId ID = ShapeId.from("aws.api#arnReference");

    private static final String TYPE = "type";
    private static final String SERVICE = "service";
    private static final String RESOURCE = "resource";

    private String type;
    private ShapeId service;
    private ShapeId resource;

    private ArnReferenceTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.type = builder.type;
        this.service = builder.service;
        this.resource = builder.resource;
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            ObjectNode objectNode = value.expectObjectNode();
            Builder builder = builder();
            objectNode.getStringMember(TYPE)
                    .map(StringNode::getValue)
                    .ifPresent(builder::type);
            objectNode.getStringMember(SERVICE)
                    .map(stringNode -> stringNode.expectShapeId(target.getNamespace()))
                    .ifPresent(builder::service);
            objectNode.getStringMember(RESOURCE)
                    .map(stringNode -> stringNode.expectShapeId(target.getNamespace()))
                    .ifPresent(builder::resource);
            return builder.build();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Get the AWS CloudFormation type of the ARN.
     *
     * @return Returns the optional type.
     */
    public Optional<String> getType() {
        return Optional.ofNullable(type);
    }

    /**
     * Get the Smithy resource shape ID of the ARN.
     *
     * @return Returns the optionally present shape ID.
     */
    public Optional<ShapeId> getResource() {
        return Optional.ofNullable(resource);
    }

    /**
     * Get the Smithy service shape ID of the ARN.
     *
     * @return Returns the optionally present shape ID.
     */
    public Optional<ShapeId> getService() {
        return Optional.ofNullable(service);
    }

    @Override
    public Builder toBuilder() {
        return new Builder()
                .sourceLocation(getSourceLocation())
                .type(type)
                .service(service)
                .resource(resource);
    }

    @Override
    protected Node createNode() {
        return Node.objectNode()
                .withOptionalMember(TYPE, getType().map(Node::from))
                .withOptionalMember(SERVICE, getService().map(ShapeId::toString).map(Node::from))
                .withOptionalMember(RESOURCE, getResource().map(ShapeId::toString).map(Node::from));
    }

    /** Builder for {@link ArnReferenceTrait}. */
    public static final class Builder extends AbstractTraitBuilder<ArnReferenceTrait, Builder> {
        private String type;
        private ShapeId service;
        private ShapeId resource;

        private Builder() {}

        @Override
        public ArnReferenceTrait build() {
            return new ArnReferenceTrait(this);
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public Builder service(ShapeId service) {
            this.service = service;
            return this;
        }

        public Builder resource(ShapeId resource) {
            this.resource = resource;
            return this;
        }
    }
}
