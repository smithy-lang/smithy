/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.aws.iam.traits;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.MapUtils;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Defines condition keys used in a service.
 */
public final class DefineConditionKeysTrait extends AbstractTrait implements ToSmithyBuilder<DefineConditionKeysTrait> {
    public static final ShapeId ID = ShapeId.from("aws.iam#defineConditionKeys");

    private final Map<String, ConditionKeyDefinition> conditionKeys;

    private DefineConditionKeysTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        conditionKeys = MapUtils.copyOf(builder.conditionKeys);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            Builder builder = builder().sourceLocation(value);
            for (Map.Entry<StringNode, Node> entry : value.expectObjectNode().getMembers().entrySet()) {
                ConditionKeyDefinition definition = ConditionKeyDefinition.fromNode(
                        entry.getValue().expectObjectNode());
                builder.putConditionKey(entry.getKey().getValue(), definition);
            }
            DefineConditionKeysTrait result = builder.build();
            result.setNodeCache(value);
            return result;
        }
    }

    /**
     * Gets all condition keys of the service.
     *
     * @return Returns the immutable map of condition key name to definition.
     */
    public Map<String, ConditionKeyDefinition> getConditionKeys() {
        return conditionKeys;
    }

    /**
     * Get a specific condition key by name.
     *
     * @param name Name of the condition key to get.
     * @return Returns the optionally found condition key.
     */
    public Optional<ConditionKeyDefinition> getConditionKey(String name) {
        return Optional.ofNullable(conditionKeys.get(name));
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = ObjectNode.builder().sourceLocation(getSourceLocation());
        for (Map.Entry<String, ConditionKeyDefinition> entry : conditionKeys.entrySet()) {
            builder.withMember(entry.getKey(), entry.getValue().toNode());
        }
        return builder.build();
    }

    @Override
    public Builder toBuilder() {
        Builder builder = builder().sourceLocation(getSourceLocation());
        conditionKeys.forEach(builder::putConditionKey);
        return builder;
    }

    public static final class Builder extends AbstractTraitBuilder<DefineConditionKeysTrait, Builder> {
        private final Map<String, ConditionKeyDefinition> conditionKeys = new HashMap<>();

        private Builder() {}

        @Override
        public DefineConditionKeysTrait build() {
            return new DefineConditionKeysTrait(this);
        }

        public Builder putConditionKey(String name, ConditionKeyDefinition definition) {
            conditionKeys.put(name, definition);
            return this;
        }

        public Builder removeConditionKey(String name) {
            conditionKeys.remove(name);
            return this;
        }
    }
}
