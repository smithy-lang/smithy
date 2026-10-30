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
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * Restricts the calls of an operation to those that satisfy one or more JMESPath
 * expressions. Each expression must produce 'true' for every call.
 */
public final class ContractsTrait extends AbstractTrait implements ToSmithyBuilder<ContractsTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.contracts#contracts");

    private final Map<String, Condition> contracts;

    private ContractsTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.contracts = builder.contracts.copy();
    }

    @Override
    protected Node createNode() {
        ObjectNode.Builder builder = ObjectNode.builder();
        for (Map.Entry<String, Condition> entry : contracts.entrySet()) {
            builder.withMember(entry.getKey(), entry.getValue().toNode());
        }
        return builder.sourceLocation(getSourceLocation()).build();
    }

    /**
     * Creates a {@link ContractsTrait} from a {@link Node}.
     *
     * @param node Node to create the ContractsTrait from.
     * @return Returns the created ContractsTrait.
     * @throws ExpectationNotMetException if the given Node is invalid.
     */
    public static ContractsTrait fromNode(Node node) {
        Builder builder = builder().sourceLocation(node);
        Map<StringNode, Node> members = node.expectObjectNode().getMembers();
        for (Map.Entry<StringNode, Node> entry : members.entrySet()) {
            Condition condition = Condition.fromNode(entry.getValue());
            String name = entry.getKey().expectStringNode().getValue();
            builder.putContract(name, condition);
        }
        return builder.build();
    }

    /**
     * Gets the map of contract names to {@link Condition}s.
     *
     * @return Returns the contracts map.
     */
    public Map<String, Condition> getContracts() {
        return contracts;
    }

    /**
     * Converts this trait to a builder used to build an equivalent {@link ContractsTrait}.
     */
    public SmithyBuilder<ContractsTrait> toBuilder() {
        return new Builder(this);
    }

    /**
     * Creates a builder used to build a {@link ContractsTrait}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ContractsTrait}.
     */
    public static final class Builder extends AbstractTraitBuilder<ContractsTrait, Builder> {
        private final BuilderRef<Map<String, Condition>> contracts = BuilderRef.forOrderedMap();

        private Builder() {}

        private Builder(ContractsTrait trait) {
            sourceLocation(trait.getSourceLocation());
            this.contracts.setBorrowed(trait.contracts);
        }

        public Builder contracts(Map<String, Condition> contracts) {
            clearContracts();
            this.contracts.get().putAll(contracts);
            return this;
        }

        public Builder clearContracts() {
            this.contracts.get().clear();
            return this;
        }

        public Builder putContract(String name, Condition condition) {
            this.contracts.get().put(name, condition);
            return this;
        }

        public Builder removeContract(String name) {
            this.contracts.get().remove(name);
            return this;
        }

        @Override
        public ContractsTrait build() {
            return new ContractsTrait(this);
        }
    }

    public static final class Provider extends AbstractTrait.Provider {
        public Provider() {
            super(ID);
        }

        @Override
        public Trait createTrait(ShapeId target, Node value) {
            ContractsTrait result = ContractsTrait.fromNode(value);
            result.setNodeCache(value);
            return result;
        }
    }
}
