/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.contracts;

import software.amazon.smithy.jmespath.JmespathException;
import software.amazon.smithy.jmespath.JmespathExpression;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.ExpectationNotMetException;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.ToSmithyBuilder;

public final class Condition implements ToNode, ToSmithyBuilder<Condition>, FromSourceLocation {
    private final SourceLocation sourceLocation;
    private final String expression;
    private final JmespathExpression parsedExpression;
    private final String description;

    private Condition(Builder builder) {
        this.sourceLocation = SmithyBuilder.requiredState("sourceLocation", builder.sourceLocation);
        this.expression = SmithyBuilder.requiredState("expression", builder.expression);
        try {
            this.parsedExpression = JmespathExpression.parse(expression);
        } catch (JmespathException e) {
            throw new SourceException(
                    "Invalid condition JMESPath expression: `" + expression + "`. " + e.getMessage(),
                    builder.sourceLocation);
        }
        this.description = SmithyBuilder.requiredState("description", builder.description);
    }

    @Override
    public Node toNode() {
        return Node.objectNodeBuilder()
                .withMember("expression", Node.from(expression))
                .withMember("description", Node.from(description))
                .build();
    }

    /**
     * Creates a {@link Condition} from a {@link Node}.
     *
     * @param node Node to create the Condition from.
     * @return Returns the created Condition.
     * @throws ExpectationNotMetException if the given Node is invalid.
     */
    public static Condition fromNode(Node node) {
        Builder builder = builder().sourceLocation(node.getSourceLocation());
        node.expectObjectNode()
                .expectStringMember("expression", builder::expression)
                .expectStringMember("description", builder::description);
        return builder.build();
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    /**
     * JMESPath expression that must evaluate to true.
     */
    public String getExpression() {
        return expression;
    }

    /**
     * The parsed expression that must evaluate to true.
     */
    public JmespathExpression getParsedExpression() {
        return parsedExpression;
    }

    /**
     * Description of the condition. Used in error messages when violated.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Creates a builder used to build a {@link Condition}.
     */
    public SmithyBuilder<Condition> toBuilder() {
        return builder()
                .expression(expression)
                .description(description);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Condition}.
     */
    public static final class Builder implements SmithyBuilder<Condition> {
        private SourceLocation sourceLocation;
        private String expression;
        private String description;

        private Builder() {}

        public Builder sourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = sourceLocation;
            return this;
        }

        public Builder expression(String expression) {
            this.expression = expression;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        @Override
        public Condition build() {
            return new Condition(this);
        }
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        } else if (!(other instanceof Condition)) {
            return false;
        } else {
            Condition b = (Condition) other;
            return toNode().equals(b.toNode());
        }
    }

    @Override
    public int hashCode() {
        return toNode().hashCode();
    }
}
