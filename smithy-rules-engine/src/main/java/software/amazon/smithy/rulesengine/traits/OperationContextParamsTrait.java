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
 * Binds elements of a target operation's input to rule-set parameters.
 */
@SmithyUnstableApi
public final class OperationContextParamsTrait extends AbstractTrait
        implements ToSmithyBuilder<OperationContextParamsTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.rules#operationContextParams");

    private final Map<String, OperationContextParamDefinition> parameters;

    private OperationContextParamsTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.parameters = builder.parameters.copy();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, OperationContextParamDefinition> getParameters() {
        return parameters;
    }

    @Override
    protected Node createNode() {
        NodeMapper mapper = new NodeMapper();
        mapper.setOmitEmptyValues(true);
        return mapper.serialize(getParameters()).expectObjectNode();
    }

    @Override
    public Builder toBuilder() {
        return new Builder()
                .sourceLocation(getSourceLocation())
                .parameters(parameters);
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            NodeMapper mapper = new NodeMapper();
            Map<String, OperationContextParamDefinition> parameters = new LinkedHashMap<>();
            value.expectObjectNode().getMembers().forEach((stringNode, node) -> {
                parameters.put(stringNode.getValue(), mapper.deserialize(node, OperationContextParamDefinition.class));
            });
            OperationContextParamsTrait trait = builder()
                    .sourceLocation(value)
                    .parameters(parameters)
                    .build();
            trait.setNodeCache(value);
            return trait;
        }
    }

    public static final class Builder extends AbstractTraitBuilder<OperationContextParamsTrait, Builder> {
        private final BuilderRef<Map<String, OperationContextParamDefinition>> parameters = BuilderRef.forOrderedMap();

        private Builder() {}

        public Builder parameters(Map<String, OperationContextParamDefinition> parameters) {
            this.parameters.clear();
            this.parameters.get().putAll(parameters);
            return this;
        }

        public Builder putParameter(String name, OperationContextParamDefinition definition) {
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
        public OperationContextParamsTrait build() {
            return new OperationContextParamsTrait(this);
        }
    }
}
