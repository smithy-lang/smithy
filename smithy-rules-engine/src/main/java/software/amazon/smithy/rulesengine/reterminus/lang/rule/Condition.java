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

package software.amazon.smithy.rulesengine.reterminus.lang.rule;

import java.util.Optional;
import software.amazon.smithy.model.FromSourceLocation;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.model.node.ToNode;
import software.amazon.smithy.rulesengine.IntoSelf;
import software.amazon.smithy.rulesengine.reterminus.SourceAwareBuilder;
import software.amazon.smithy.rulesengine.reterminus.eval.Eval;
import software.amazon.smithy.rulesengine.reterminus.eval.Scope;
import software.amazon.smithy.rulesengine.reterminus.eval.Type;
import software.amazon.smithy.rulesengine.reterminus.eval.Typecheck;
import software.amazon.smithy.rulesengine.reterminus.eval.Value;
import software.amazon.smithy.rulesengine.reterminus.lang.Identifier;
import software.amazon.smithy.rulesengine.reterminus.lang.expr.Expr;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.Fn;
import software.amazon.smithy.rulesengine.reterminus.lang.fn.FnNode;
import software.amazon.smithy.utils.SmithyBuilder;

public final class Condition implements Typecheck, Eval, FromSourceLocation, ToNode, IntoSelf<Condition> {
    public static final String ASSIGN = "assign";
    private final Expr fn;
    private final Identifier result;

    private Condition(Builder builder) {
        this.result = builder.result;
        this.fn = SmithyBuilder.requiredState("fn", builder.fn);
    }

    public static Condition fromNode(Node node) {
        ObjectNode on = node.expectObjectNode("condition must be an object node");
        Fn fn = FnNode.fromNode(on).validate();
        Optional<String> result = on.getStringMember(ASSIGN).map(StringNode::getValue);
        Builder builder = new Builder();
        result.ifPresent(builder::result);
        builder.fn(fn);
        return builder.build();
    }

    public Expr getFn() {
        return fn;
    }

    @Override
    public SourceLocation getSourceLocation() {
        return fn.getSourceLocation();
    }

    public Optional<Identifier> getResult() {
        return Optional.ofNullable(result);
    }

    @Override
    public Type typecheck(Scope<Type> scope) {
        Type conditionType = fn.typecheck(scope);
        // If the condition is validated, then the expression must be a truthy type
        getResult().ifPresent(resultName -> scope.insert(resultName, conditionType.provenTruthy()));
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
    public Value eval(Scope<Value> scope) {
        Value value = this.fn.eval(scope);
        if (!value.isNone()) {
            this.getResult().ifPresent(res -> scope.insert(res, value));
        }
        return value;
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder conditionNode = fn.toNode().expectObjectNode().toBuilder();
        if (result != null) {
            conditionNode.withMember(ASSIGN, result.getName());
        }
        return conditionNode.build();
    }

    public Expr expr() {
        if (this.getResult().isPresent()) {
            return Expr.ref(this.getResult().get(), SourceAwareBuilder.javaLocation());
        } else {
            throw new RuntimeException("Cannot generate expr from a condition without a result");
        }
    }


    public static class Builder implements SmithyBuilder<Condition> {
        private Fn fn;
        private Identifier result;

        public Builder fn(Fn fn) {
            this.fn = fn;
            return this;
        }

        public Builder result(String result) {
            this.result = Identifier.of(result);
            return this;
        }

        public Condition build() {
            return new Condition(this);
        }

    }
}
