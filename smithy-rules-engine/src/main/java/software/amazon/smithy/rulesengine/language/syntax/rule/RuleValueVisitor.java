/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.rule;

import java.util.List;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Visitor for the right-hand side of rules (tree, error, endpoint).
 *
 * @param <R> The return type of the visitor
 */
@SmithyUnstableApi
public interface RuleValueVisitor<R> {
    /**
     * Invoked when reaching a tree rule.
     *
     * @param rules the sub-rules within a tree rule.
     * @return the visitor return type.
     */
    R visitTreeRule(List<Rule> rules);

    /**
     * Invoked when reaching an error rule.
     *
     * @param error the error expression for the rule.
     * @return the visitor return type.
     */
    R visitErrorRule(Expression error);

    /**
     * Invoked when reaching an endpoint rule.
     *
     * @param endpoint the endpoint of the rule.
     * @return the visitor return type.
     */
    R visitEndpointRule(Endpoint endpoint);
}
