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

package software.amazon.smithy.rulesengine.traits;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Binds a Structure member shape to a rule-set parameter.
 */
@SmithyUnstableApi
public final class ContextParamTrait extends AbstractTrait implements ToSmithyBuilder<ContextParamTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.rules#contextParam");

    private final String name;

    private ContextParamTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.name = SmithyBuilder.requiredState("name", builder.name);
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getName() {
        return this.name;
    }

    @Override
    protected Node createNode() {
        NodeMapper mapper = new NodeMapper();
        mapper.disableToNodeForClass(ContextParamTrait.class);
        mapper.setOmitEmptyValues(true);
        return mapper.serialize(this).expectObjectNode();
    }

    @Override
    public SmithyBuilder<ContextParamTrait> toBuilder() {
        return new Builder()
                .sourceLocation(getSourceLocation())
                .name(name);
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            NodeMapper mapper = new NodeMapper();
            mapper.disableFromNodeForClass(ContextParamTrait.class);
            mapper.setOmitEmptyValues(true);
            ContextParamTrait trait = mapper.deserialize(value, ContextParamTrait.class);
            trait.setNodeCache(value);
            return trait;
        }
    }

    public static final class Builder extends AbstractTraitBuilder<ContextParamTrait, Builder> {
        private String name;

        private Builder() {
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        @Override
        public ContextParamTrait build() {
            return new ContextParamTrait(this);
        }
    }
}
