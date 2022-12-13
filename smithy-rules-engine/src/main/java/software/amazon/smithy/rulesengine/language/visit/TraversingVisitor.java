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

package software.amazon.smithy.rulesengine.language.visit;

import java.util.List;
import java.util.stream.Stream;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expression;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A visitor for traversing the rules and conditions of a rule-set.
 *
 * @param <R> the return type.
 */
@SmithyUnstableApi
public abstract class TraversingVisitor<R> extends DefaultVisitor<Stream<R>> {

    /**
     * Given an {@link EndpointRuleSet} will invoke the visitor methods for each rule.
     *
     * @param ruleset the endpoint rule-set to traverse.
     * @return a stream of values.
     */
    public Stream<R> visitRuleset(EndpointRuleSet ruleset) {
        return ruleset.getRules()
                .stream()
                .flatMap(this::handleRule);
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
                endpoint.getUrl()
                        .accept(this),
                endpoint.getProperties()
                        .entrySet()
                        .stream()
                        .flatMap(map -> map.getValue().accept(this))
        );
    }

    /**
     * {@link Endpoint} visitor method.
     *
     * @param conditions the conditions to visit.
     * @return a stream of values.
     */
    public Stream<R> visitConditions(List<Condition> conditions) {
        return conditions.stream().flatMap(c -> c.getFn().accept(this));
    }
}
