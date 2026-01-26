/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.contracts;

import java.util.Map;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.AbstractTrait;
import software.amazon.smithy.model.traits.AbstractTraitBuilder;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyGenerated;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Restricts shape values to those that satisfy one or more JMESPath expressions.
 * Each expression must produce 'true'.
 */
@SmithyGenerated
public final class ConditionsTrait extends AbstractTrait implements ToSmithyBuilder<ConditionsTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.contracts#conditions");

    private final Map<String, Condition> conditions;

    private ConditionsTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.conditions = builder.conditions.copy();
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = ObjectNode.builder();
        for (Map.Entry<String, Condition> entry : conditions.entrySet()) {
            builder.withMember(entry.getKey(), entry.getValue().toNode());
        }
        return builder.sourceLocation(getSourceLocation()).build();
    }

    /**
     * Creates a {@link ConditionsTrait} from a {@link Node}.
     *
     * @param node Node to create the ConditionsTrait from.
     * @return Returns the created ConditionsTrait.
     * @throws ExpectationNotMetException if the given Node is invalid.
     */
    public static ConditionsTrait fromNode(Node node) {
        Builder builder = builder().sourceLocation(node);
        Map<StringNode, Node> members = node.expectObjectNode().getMembers();
        for (Map.Entry<StringNode, Node> entry : members.entrySet()) {
            Condition condition = Condition.fromNode(entry.getValue());
            String name = entry.getKey().expectStringNode().getValue();
            builder.putCondition(name, condition);
        }
        return builder.build();
    }

    public Map<String, Condition> getConditions() {
        return conditions;
    }

    /**
     * Creates a builder used to build a {@link ConditionsTrait}.
     */
    public SmithyBuilder<ConditionsTrait> toBuilder() {
        return builder().sourceLocation(getSourceLocation())
                .conditions(getConditions());
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ConditionsTrait}.
     */
    public static final class Builder extends AbstractTraitBuilder<ConditionsTrait, Builder> {
        private final BuilderRef<Map<String, Condition>> conditions = BuilderRef.forOrderedMap();

        private Builder() {}

        public Builder conditions(Map<String, Condition> conditions) {
            clear();
            this.conditions.get().putAll(conditions);
            return this;
        }

        public Builder clear() {
            this.conditions.get().clear();
            return this;
        }

        public Builder putCondition(String name, Condition condition) {
            this.conditions.get().put(name, condition);
            return this;
        }

        public Builder removeCondition(String name) {
            this.conditions.get().remove(name);
            return this;
        }

        @Override
        public ConditionsTrait build() {
            return new ConditionsTrait(this);
        }
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            ConditionsTrait result = ConditionsTrait.fromNode(value);
            result.setNodeCache(value);
            return result;
        }
    }
}
