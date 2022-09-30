/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.rulesengine.language.syntax.rule;

import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.IntoSelf;
import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.eval.TypeCheck;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expression;
import software.amazon.smithy.rulesengine.language.syntax.fn.FunctionNode;
import software.amazon.smithy.rulesengine.language.util.SourceLocationUtils;
import software.amazon.smithy.utils.SmithyBuilder;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A condition is call to a rule-set function that used to determine whether a rule should be executed.
 * Can assign the results of functions to new parameters within the current scope.
 */
@SmithyUnstableApi
public final class Condition implements TypeCheck, FromSourceLocation, ToNode, IntoSelf<Condition> {
    public static final String ASSIGN = "assign";
    private final Expression fn;
    private final Identifier result;

    private Condition(Builder builder) {
        this.result = builder.result;
        this.fn = SmithyBuilder.requiredState("fn", builder.fn);
    }

    /**
     * Constructs a condition from the given node.
     *
     * @param node the node.
     * @return the condition instance.
     */
    public static Condition fromNode(Node node) {
        ObjectNode on = node.expectObjectNode("condition must be an object node");
        Expression fn = FunctionNode.fromNode(on).validate();
        Optional<Identifier> result = on.getStringMember(ASSIGN).map(Identifier::of);
        Builder builder = new Builder();
        result.ifPresent(builder::result);
        builder.fn(fn);
        return builder.build();
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
        return Objects.equals(fn, condition.fn) && Objects.equals(result, condition.result);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fn, result);
    }

    public Expression getFn() {
        return fn;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return fn.getSourceLocation();
    }

    /**
     * Get the identifier of the parameter that the result is assigned to.
     *
     * @return the optional identifier.
     */
    public Optional<Identifier> getResult() {
        return Optional.ofNullable(result);
    }

    @Override
    public Type typeCheck(Scope<Type> scope) {
        Type conditionType = fn.typeCheck(scope);
        // If the condition is validated, then the expression must be a truthy type
        getResult().ifPresent(resultName -> {
            scope.getDeclaration(resultName).ifPresent(entry -> {
                throw new SourceException(String.format("Invalid shadowing of `%s` (first declared on line %s)",
                        resultName, entry.getKey().getSourceLocation().getLine()), resultName);
            });
            scope.insert(resultName, conditionType.provenTruthy());
        });
        return conditionType;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        this.getResult().ifPresent(res -> sb.append(res).append(" = "));
        sb.append(this.fn);
        return sb.toString();
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder conditionNode = fn.toNode().expectObjectNode().toBuilder();
        if (result != null) {
            conditionNode.withMember(ASSIGN, result.getName());
        }
        return conditionNode.build();
    }

    /**
     * Converts this condition to an expression reference if the condition has a result assignment. Otherwise throws
     * an exception.
     *
     * @return the result as a reference expression.
     */
    public Expression toExpression() {
        if (this.getResult().isPresent()) {
            return Expression.reference(this.getResult().get(), SourceLocationUtils.javaLocation());
        } else {
            throw new RuntimeException("Cannot generate expression from a condition without a result");
        }
    }

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
