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

package software.amazon.smithy.model.traits;

import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Configures how a structure member maps to a resource property.
 */
public final class PropertyTrait extends AbstractTrait implements ToSmithyBuilder<PropertyTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.api#property");
    private final String name;

    private PropertyTrait(Builder builder) {
        super(ID, builder.sourceLocation);
        name = builder.name;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    @Override
    protected Node createNode() {
        return Node.objectNodeBuilder()
                .sourceLocation(getSourceLocation())
                .withOptionalMember("name", getName().map(Node::from))
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public Builder toBuilder() {
        return builder().sourceLocation(getSourceLocation()).name(name);
    }

    public static final class Builder extends AbstractTraitBuilder<PropertyTrait, Builder> {
        private String name;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public PropertyTrait build() {
            return new PropertyTrait(this);
        }
    }

    public static final class Provider implements TraitService {
        @Override
        public ShapeId getShapeId() {
            return ID;
        }

        @Override
        public PropertyTrait createTrait(ShapeId target, Node value) {
            ObjectNode objectNode = value.expectObjectNode();
            String name = objectNode.getStringMemberOrDefault("name", null);
            PropertyTrait result = builder().sourceLocation(value).name(name).build();
            result.setNodeCache(value);
            return result;
        }
    }
}
