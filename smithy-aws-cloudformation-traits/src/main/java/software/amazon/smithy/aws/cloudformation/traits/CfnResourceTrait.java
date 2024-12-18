/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.cloudformation.traits;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Indicates that a Smithy resource is a CloudFormation resource.
 */
public final class CfnResourceTrait extends AbstractTrait
        implements ToSmithyBuilder<CfnResourceTrait> {
    public static final ShapeId ID = ShapeId.from("aws.cloudformation#cfnResource");

    private final String name;
    private final List<ShapeId> additionalSchemas;

    private CfnResourceTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        name = builder.name;
        additionalSchemas = ListUtils.copyOf(builder.additionalSchemas);
    }

    /**
     * Get the AWS CloudFormation resource name.
     *
     * @return Returns the name.
     */
    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    /**
     * Get the Smithy structure shape Ids for additional schema properties.
     *
     * @return Returns the additional schema shape Ids.
     */
    public List<ShapeId> getAdditionalSchemas() {
        return additionalSchemas;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected Node createNode() {
        NodeMapper mapper = new NodeMapper();
        mapper.disableToNodeForClass(CfnResourceTrait.class);
        mapper.setOmitEmptyValues(true);
        return mapper.serialize(this).expectObjectNode();
    }

    @Override
    public Builder toBuilder() {
        return builder().sourceLocation(getSourceLocation()).name(name).additionalSchemas(additionalSchemas);
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            CfnResourceTrait result = new NodeMapper().deserialize(value, CfnResourceTrait.class);
            result.setNodeCache(value);
            return result;
        }
    }

    public static final class Builder extends AbstractTraitBuilder<CfnResourceTrait, Builder> {
        private String name;
        private final List<ShapeId> additionalSchemas = new ArrayList<>();

        private Builder() {}

        @Override
        public CfnResourceTrait build() {
            return new CfnResourceTrait(this);
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder addAdditionalSchema(ShapeId additionalSchema) {
            this.additionalSchemas.add(additionalSchema);
            return this;
        }

        public Builder additionalSchemas(List<ShapeId> additionalSchemas) {
            this.additionalSchemas.clear();
            this.additionalSchemas.addAll(additionalSchemas);
            return this;
        }
    }
}
