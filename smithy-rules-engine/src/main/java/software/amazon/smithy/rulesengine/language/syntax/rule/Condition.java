/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.rule;

import static software.amazon.smithy.rulesengine.language.RulesComponentBuilder.javaLocation;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.TypeCheck;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.SyntaxElement;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionNode;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A condition is call to a rule-set function that used to determine whether a rule should be executed.
 * Can assign the results of functions to new parameters within the current scope.
 */
@SmithyUnstableApi
public final class Condition extends SyntaxElement implements TypeCheck, FromSourceLocation, ToNode {
    public static final String ASSIGN = "assign";
    private final Expression function;
    private final Identifier result;

    private Condition(Builder builder) {
        this.result = builder.result;
        this.function = SmithyBuilder.requiredState("fn", builder.fn);
    }

    /**
     * Builder to create a {@link Condition} instance.
     *
     * @return returns a new Builder.
     */
    public static Builder builder() {
        return new Condition.Builder();
    }

    /**
     * Constructs a condition from the given node.
     *
     * @param node the node.
     * @return the condition instance.
     */
    public static Condition fromNode(Node node) {
        Builder builder = new Builder();
        ObjectNode objectNode = node.expectObjectNode("condition must be an object node");

        builder.fn(FunctionNode.fromNode(objectNode).createFunction());
        // This needs to go directly through the node to maintain source locations.
        if (objectNode.containsMember(ASSIGN)) {
            builder.result(Identifier.of(objectNode.expectStringMember(ASSIGN)));
        }

        return builder.build();
    }

    @Override
    public SourceLocation getSourceLocation() {
        return function.getSourceLocation();
    }

    /**
     * Get the identifier of the parameter that the result is assigned to.
     *
     * @return the optional identifier.
     */
    public Optional<Identifier> getResult() {
        return Optional.ofNullable(result);
    }

    /**
     * Gets the function used to express this condition.
     *
     * @return the function for this condition.
     */
    public Expression getFunction() {
        return function;
    }

    @Override
    public Builder toConditionBuilder() {
        return toBuilder();
    }

    @Override
    public Condition toCondition() {
        return this;
    }

    @Override
    public Expression toExpression() {
        if (result == null) {
            throw new RuntimeException("Cannot generate expression from a condition without a result");
        }
        return Expression.getReference(result, javaLocation());
    }

    public Builder toBuilder() {
        return builder()
                .fn(function)
                .result(result);
    }

    @Override
    public Type typeCheck(Scope<Type> scope) {
        Type conditionType = function.typeCheck(scope);
        // If the condition is validated, then the expression must be a truthy type
        if (result != null) {
            scope.getDeclaration(result).ifPresent(entry -> {
                throw new SourceException(String.format("Invalid shadowing of `%s` (first declared on line %s)",
                        result, entry.getKey().getSourceLocation().getLine()), result);
            });
            scope.insert(result, conditionType.provenTruthy());
        }
        return conditionType;
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder conditionNode = function.toNode().expectObjectNode().toBuilder();
        if (result != null) {
            conditionNode.withMember(ASSIGN, result.getName());
        }
        return conditionNode.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Condition condition = (Condition) o;
        return Objects.equals(function, condition.function) && Objects.equals(result, condition.result);
    }

    @Override
    public int hashCode() {
        return Objects.hash(function, result);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (result != null) {
            sb.append(result).append(" = ");
        }
        return sb.append(function).toString();
    }

    /**
     * A builder used to create a {@link Condition} class.
     */
    public static class Builder implements SmithyBuilder<Condition> {
        private Expression fn;
        private Identifier result;

        public Builder fn(Expression fn) {
            this.fn = fn;
            return this;
        }

        public Builder result(String result) {
            this.result = Identifier.of(result);
            return this;
        }

        public Builder result(Identifier result) {
            this.result = result;
            return this;
        }

        public Condition build() {
            return new Condition(this);
        }
    }
}
