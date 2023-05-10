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

package software.amazon.smithy.rulesengine.language.eval;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expr.Literal;
import software.amazon.smithy.rulesengine.language.syntax.expr.Reference;
import software.amazon.smithy.rulesengine.language.syntax.fn.FunctionDefinition;
import software.amazon.smithy.rulesengine.language.syntax.fn.GetAttr;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.visit.ExpressionVisitor;
import software.amazon.smithy.rulesengine.language.visit.RuleValueVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A runtime implementation of a rule-set evaluation engine.
 */
@SmithyUnstableApi
public class RuleEvaluator implements ExpressionVisitor<Value> {
    private final Scope<Value> scope = new Scope<>();

    /**
     * Initializes a new {@link RuleEvaluator} instances, and evaluates the provided ruleset and parameter arguments.
     *
     * @param ruleset            The endpoint ruleset.
     * @param parameterArguments The rule-set parameter identifiers and values to evaluate the rule-set against.
     * @return The resulting value from the final matched rule.
     */
    public static Value evaluate(EndpointRuleSet ruleset, Map<Identifier, Value> parameterArguments) {
        return new RuleEvaluator().evaluateRuleSet(ruleset, parameterArguments);
    }

    /**
     * Evaluate the provided ruleset and parameter arguments.
     *
     * @param ruleset            The endpoint ruleset.
     * @param parameterArguments The rule-set parameter identifiers and values to evaluate the rule-set against.
     * @return The resulting value from the final matched rule.
     */
    public Value evaluateRuleSet(EndpointRuleSet ruleset, Map<Identifier, Value> parameterArguments) {
        return scope.inScope(
                () -> {
                    ruleset
                            .getParameters()
                            .toList()
                            .forEach(
                                    param -> {
                                        param.getDefault().ifPresent(value -> scope.insert(param.getName(), value));
                                    });
                    parameterArguments.forEach(scope::insert);
                    for (Rule rule : ruleset.getRules()) {
                        Value result = handleRule(rule);
                        if (!result.isNone()) {
                            return result;
                        }
                    }
                    throw new RuntimeException("No rules in ruleset matched");
                });
    }

    @Override
    public Value visitLiteral(Literal literal) {
        return literal.evaluate(this);
    }

    @Override
    public Value visitRef(Reference reference) {
        return scope
                .getValue(reference.getName())
                .orElse(Value.none());
    }

    @Override
    public Value visitIsSet(Expression fn) {
        return Value.bool(!fn.accept(this).isNone());
    }

    @Override
    public Value visitNot(Expression not) {
        return Value.bool(!not.accept(this).expectBool());
    }

    @Override
    public Value visitBoolEquals(Expression left, Expression right) {
        return Value.bool(left.accept(this).expectBool() == right.accept(this).expectBool());
    }

    @Override
    public Value visitStringEquals(Expression left, Expression right) {
        return Value.bool(left.accept(this).expectString().equals(right.accept(this).expectString()));
    }

    public Value visitGetAttr(GetAttr getAttr) {
        return getAttr.evaluate(getAttr.getTarget().accept(this));
    }

    @Override
    public Value visitLibraryFunction(FunctionDefinition definition, List<Expression> arguments) {
        return definition.evaluate(arguments.stream().map(arg -> arg.accept(this)).collect(Collectors.toList()));
    }

    private Value handleRule(Rule rule) {
        return scope.inScope(() -> {
            for (Condition condition : rule.getConditions()) {
                Value value = evaluateCondition(condition);
                if (value.isNone() || value.equals(Value.bool(false))) {
                    return Value.none();
                }
            }
            return rule.accept(new RuleValueVisitor<Value>() {
                @Override
                public Value visitTreeRule(List<Rule> rules) {
                    for (Rule subRule : rules) {
                        Value result = handleRule(subRule);
                        if (!result.isNone()) {
                            return result;
                        }
                    }
                    throw new RuntimeException(
                            String.format("no rules inside of tree rule matched—invalid rules (%s)", rule));
                }

                @Override
                public Value visitErrorRule(Expression error) {
                    return RuleEvaluator.this.visitErrorRule(error);
                }

                @Override
                public Value visitEndpointRule(Endpoint endpoint) {
                    return RuleEvaluator.this.visitEndpointRule(endpoint);
                }
            });
        });
    }

    public Value visitErrorRule(Expression error) {
        return error.accept(this);
    }

    public Value visitEndpointRule(Endpoint endpoint) {
        Value.Endpoint.Builder builder = Value.Endpoint.builder()
                .sourceLocation(endpoint)
                .url(endpoint.getUrl()
                        .accept(RuleEvaluator.this)
                        .expectString());
        endpoint.getProperties()
                .forEach((key, value) -> builder.addProperty(key.toString(),
                        value.accept(RuleEvaluator.this)));
        endpoint.getHeaders()
                .forEach((name, expressions) -> expressions.forEach(expr -> builder.addHeader(name,
                        expr.accept(RuleEvaluator.this).expectString())));
        return builder.build();

    }

    public Value evaluateCondition(Condition condition) {
        Value value = condition.getFn().accept(this);
        if (!value.isNone()) {
            condition.getResult().ifPresent(res -> scope.insert(res, value));
        }
        return value;
    }
}
