/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.traits;

import java.util.Optional;
import software.amazon.smithy.model.node.Node;
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
            Builder builder = builder().sourceLocation(value.getSourceLocation());
            value.expectObjectNode().getStringMember("name", builder::name);
            PropertyTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }
}
