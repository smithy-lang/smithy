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
import software.amazon.smithy.rulesengine.language.EndpointRuleset;
import software.amazon.smithy.rulesengine.language.lang.expr.Expr;
import software.amazon.smithy.rulesengine.language.lang.fn.Fn;
import software.amazon.smithy.rulesengine.language.lang.rule.Condition;
import software.amazon.smithy.rulesengine.language.lang.rule.Rule;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public abstract class TraversingVisitor<R> extends DefaultVisitor<Stream<R>> {
    public Stream<R> visitRuleset(EndpointRuleset ruleset) {
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
    public Stream<R> visitFn(Fn fn) {
        return fn.acceptFnVisitor(this);
    }

    @Override
    public Stream<R> visitTreeRule(List<Rule> rules) {
        return rules.stream().flatMap(this::handleRule);
    }

    @Override
    public Stream<R> visitErrorRule(Expr error) {
        return error.accept(this);
    }

    @Override
    public Stream<R> visitEndpointRule(Endpoint endpoint) {
        return visitEndpoint(endpoint);
    }

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

    public Stream<R> visitConditions(List<Condition> conditions) {
        return conditions.stream().flatMap(c -> c.getFn().accept(this));
    }
}
