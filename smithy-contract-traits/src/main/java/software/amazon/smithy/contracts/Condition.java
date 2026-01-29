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

/**
 * Defines an individual condition.
 */
public final class Condition implements ToNode, ToSmithyBuilder<Condition>, FromSourceLocation {
    private final SourceLocation sourceLocation;
    private final String expressionText;
    private final JmespathExpression expression;
    private final String documentation;

    private Condition(Builder builder) {
        this.sourceLocation = SmithyBuilder.requiredState("sourceLocation", builder.sourceLocation);
        this.expressionText = SmithyBuilder.requiredState("expression", builder.expression);
        try {
            this.expression = JmespathExpression.parse(expressionText);
        } catch (JmespathException e) {
            throw new SourceException(
                    "Invalid condition JMESPath expression: `" + expressionText + "`. " + e.getMessage(),
                    builder.sourceLocation);
        }
        this.documentation = SmithyBuilder.requiredState("documentation", builder.documentation);
    }

    @Override
    public Node toNode() {
        return Node.objectNodeBuilder()
                .withMember("expression", Node.from(expressionText))
                .withMember("documentation", Node.from(documentation))
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
                .expectStringMember("documentation", builder::documentation);
        return builder.build();
    }

    @Override
    public SourceLocation getSourceLocation() {
        return sourceLocation;
    }

    /**
     * JMESPath expression that must evaluate to true.
     *
     * @return Return the JMESPath expression.
     */
    public JmespathExpression getExpression() {
        return expression;
    }

    /**
     * Documentation about the condition.
     *
     * @return Return the documentation.
     */
    public String getDocumentation() {
        return documentation;
    }

    @Override
    public Builder toBuilder() {
        return builder()
                .expression(expressionText)
                .documentation(documentation);
    }

    /**
     * Creates a builder used to build an equivalent {@link Condition}.
     * @return the builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Condition}.
     */
    public static final class Builder implements SmithyBuilder<Condition> {
        private SourceLocation sourceLocation;
        private String expression;
        private String documentation;

        private Builder() {}

        public Builder sourceLocation(SourceLocation sourceLocation) {
            this.sourceLocation = sourceLocation;
            return this;
        }

        public Builder expression(String expression) {
            this.expression = expression;
            return this;
        }

        public Builder documentation(String documentation) {
            this.documentation = documentation;
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
