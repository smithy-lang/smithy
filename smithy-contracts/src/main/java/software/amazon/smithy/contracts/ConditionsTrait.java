/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.contracts;

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
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

    private final List<Condition> conditions;

    private ConditionsTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.conditions = builder.conditions.copy();
    }

    @Override
    protected Node createNode() {
        ArrayNode.Builder builder = ArrayNode.builder();
        for (Condition condition : conditions) {
            builder.withValue(condition.toNode());
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
        List<Node> elements = node.expectArrayNode().getElements();
        List<Condition> conditions = new ArrayList<>();
        for (Node element : elements) {
            Condition condition = Condition.fromNode(element);
            conditions.add(condition);
        }
        builder.conditions(conditions);
        return builder.build();
    }

    public List<Condition> getConditions() {
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
        private final BuilderRef<List<Condition>> conditions = BuilderRef.forList();

        private Builder() {}

        public Builder conditions(List<Condition> conditions) {
            clearConditions();
            this.conditions.get().addAll(conditions);
            return this;
        }

        public Builder clearConditions() {
            this.conditions.get().clear();
            return this;
        }

        public Builder addConditions(Condition value) {
            this.conditions.get().add(value);
            return this;
        }

        public Builder removeConditions(Condition value) {
            this.conditions.get().remove(value);
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
