/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.traits;

import java.util.LinkedHashMap;
import java.util.Map;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.NodeMapper;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Indicates that the named rule-set parameters that should be configurable
 * on the service client using the specified smithy types.
 */
@SmithyUnstableApi
public final class ClientContextParamsTrait extends AbstractTrait implements ToSmithyBuilder<ClientContextParamsTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.rules#clientContextParams");

    private final Map<String, ClientContextParamDefinition> parameters;

    public ClientContextParamsTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.parameters = builder.parameters.copy();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, ClientContextParamDefinition> getParameters() {
        return parameters;
    }

    @Override
    protected Node createNode() {
        NodeMapper mapper = new NodeMapper();
        return mapper.serialize(this.getParameters()).expectObjectNode();
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .sourceLocation(getSourceLocation())
                .parameters(getParameters());
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            NodeMapper mapper = new NodeMapper();

            Map<String, ClientContextParamDefinition> parameters = new LinkedHashMap<>();
            value.expectObjectNode().getMembers().forEach((stringNode, node) -> {
                parameters.put(stringNode.getValue(), mapper.deserialize(node, ClientContextParamDefinition.class));
            });

            ClientContextParamsTrait trait = builder()
                    .parameters(parameters)
                    .sourceLocation(value)
                    .build();
            trait.setNodeCache(value);
            return trait;
        }
    }

    public static final class Builder extends AbstractTraitBuilder<ClientContextParamsTrait, Builder> {
        private final BuilderRef<Map<String, ClientContextParamDefinition>> parameters = BuilderRef.forOrderedMap();

        private Builder() {
        }

        public Builder parameters(Map<String, ClientContextParamDefinition> parameters) {
            this.parameters.clear();
            this.parameters.get().putAll(parameters);
            return this;
        }

        public Builder putParameter(String name, ClientContextParamDefinition definition) {
            this.parameters.get().put(name, definition);
            return this;
        }

        public Builder removeParameter(String name) {
            this.parameters.get().remove(name);
            return this;
        }

        public Builder clearParameters() {
            this.parameters.clear();
            return this;
        }

        @Override
        public ClientContextParamsTrait build() {
            return new ClientContextParamsTrait(this);
        }
    }
}
