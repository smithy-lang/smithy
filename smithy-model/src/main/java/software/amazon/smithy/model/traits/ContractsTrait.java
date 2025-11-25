/**
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.model.traits;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.utils.BuilderRef;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyGenerated;
import software.amazon.smithy.utils.ToSmithyBuilder;

/**
 * TODO: These expressions must produce 'true'...
 */
@SmithyGenerated
public final class ContractsTrait extends AbstractTrait implements ToSmithyBuilder<ContractsTrait> {
    public static final ShapeId ID = ShapeId.from("smithy.contracts#contracts");

    private final Map<String, Contract> values;

    private ContractsTrait(Builder builder) {
        super(ID, builder.getSourceLocation());
        this.values = builder.values.copy();
    }

    @Override
    protected Node createNode() {
        return values.entrySet().stream()
            .map(entry -> new SimpleImmutableEntry<>(
                Node.from(entry.getKey()), entry.getValue().toNode()))
            .collect(ObjectNode.collect(Entry::getKey, Entry::getValue))
            .toBuilder().sourceLocation(getSourceLocation()).build();
    }

    /**
     * Creates a {@link ContractsTrait} from a {@link Node}.
     *
     * @param node Node to create the ConstraintsTrait from.
     * @return Returns the created ConstraintsTrait.
     * @throws ExpectationNotMetException if the given Node is invalid.
     */
    public static ContractsTrait fromNode(Node node) {
        Builder builder = builder();
        node.expectObjectNode().getMembers().forEach((k, v) -> {
            builder.putValues(k.expectStringNode().getValue(), Contract.fromNode(v));
        });
        return builder.build();
    }

    /**
     * These expressions must produce 'true'
     */
    public Map<String, Contract> getValues() {
        return values;
    }

    /**
     * Creates a builder used to build a {@link ContractsTrait}.
     */
    public SmithyBuilder<ContractsTrait> toBuilder() {
        return builder().sourceLocation(getSourceLocation())
            .values(values);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ContractsTrait}.
     */
    public static final class Builder extends AbstractTraitBuilder<ContractsTrait, Builder> {
        private final BuilderRef<Map<String, Contract>> values = BuilderRef.forOrderedMap();

        private Builder() {}

        public Builder values(Map<String, Contract> values) {
            clearValues();
            this.values.get().putAll(values);
            return this;
        }

        public Builder clearValues() {
            this.values.get().clear();
            return this;
        }

        public Builder putValues(String key, Contract value) {
            this.values.get().put(key, value);
            return this;
        }

        public Builder removeValues(String values) {
            this.values.get().remove(values);
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

    @SmithyGenerated
    public static final class Contract implements ToNode, ToSmithyBuilder<Contract> {
        private final String expression;
        private final String description;

        private Contract(Builder builder) {
            this.expression = SmithyBuilder.requiredState("expression", builder.expression);
            this.description = builder.description;
        }

        @Override
        public Node toNode() {
            return Node.objectNodeBuilder()
                .withMember("expression", Node.from(expression))
                .withOptionalMember("description", getDescription().map(m -> Node.from(m)))
                .build();
        }

        /**
         * Creates a {@link Contract} from a {@link Node}.
         *
         * @param node Node to create the Constraint from.
         * @return Returns the created Constraint.
         * @throws ExpectationNotMetException if the given Node is invalid.
         */
        public static Contract fromNode(Node node) {
            Builder builder = builder();
            node.expectObjectNode()
                .expectStringMember("expression", builder::expression)
                .getStringMember("description", builder::description);

            return builder.build();
        }

        /**
         * JMESPath expression that must evaluate to true.
         */
        public String getExpression() {
            return expression;
        }

        /**
         * Description of the constraint. Used in error messages when violated.
         */
        public Optional<String> getDescription() {
            return Optional.ofNullable(description);
        }

        /**
         * Creates a builder used to build a {@link Contract}.
         */
        public SmithyBuilder<Contract> toBuilder() {
            return builder()
                .expression(expression)
                .description(description);
        }

        public static Builder builder() {
            return new Builder();
        }

        /**
         * Builder for {@link Contract}.
         */
        public static final class Builder implements SmithyBuilder<Contract> {
            private String expression;
            private String description;

            private Builder() {}

            public Builder expression(String expression) {
                this.expression = expression;
                return this;
            }

            public Builder description(String description) {
                this.description = description;
                return this;
            }

            @Override
            public Contract build() {
                return new Contract(this);
            }
        }

        @Override
        public boolean equals(Object other) {
            if (other == this) {
                return true;
            } else if (!(other instanceof Contract)) {
                return false;
            } else {
                Contract b = (Contract) other;
                return toNode().equals(b.toNode());
            }
        }

        @Override
        public int hashCode() {
            return toNode().hashCode();
        }
    }
}
