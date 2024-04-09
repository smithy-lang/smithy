/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language;

import java.util.List;
import java.util.stream.Stream;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.ExpressionVisitor;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.RuleValueVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A visitor for traversing the rules and conditions of a rule-set.
 *
 * @param <R> the return type.
 */
@SmithyUnstableApi
public class TraversingVisitor<R> extends ExpressionVisitor.Default<Stream<R>>
        implements RuleValueVisitor<Stream<R>> {

    /**
     * Given an {@link EndpointRuleSet} will invoke the visitor methods for each rule.
     *
     * @param ruleset the endpoint rule-set to traverse.
     * @return a stream of values.
     */
    public Stream<R> visitRuleset(EndpointRuleSet ruleset) {
        return ruleset.getRules().stream().flatMap(this::handleRule);
    }

    private Stream<R> handleRule(Rule rule) {
        Stream<R> fromConditions = visitConditions(rule.getConditions());
        return Stream.concat(fromConditions, rule.accept(this));
    }

    @Override
    public Stream<R> getDefault() {
        return Stream.empty();
    }

    @Override
    public Stream<R> visitTreeRule(List<Rule> rules) {
        return rules.stream().flatMap(this::handleRule);
    }

    @Override
    public Stream<R> visitErrorRule(Expression error) {
        return error.accept(this);
    }

    @Override
    public Stream<R> visitEndpointRule(Endpoint endpoint) {
        return visitEndpoint(endpoint);
    }

    /**
     * {@link Endpoint} visitor method.
     *
     * @param endpoint the endpoint to visit.
     * @return a stream of values.
     */
    public Stream<R> visitEndpoint(Endpoint endpoint) {
        return Stream.concat(
                endpoint.getUrl().accept(this),
                endpoint.getProperties().values().stream().flatMap(value -> value.accept(this)));
    }

    /**
     * {@link Endpoint} visitor method.
     *
     * @param conditions the conditions to visit.
     * @return a stream of values.
     */
    public Stream<R> visitConditions(List<Condition> conditions) {
        return conditions.stream().flatMap(condition -> condition.getFunction().accept(this));
    }
}
