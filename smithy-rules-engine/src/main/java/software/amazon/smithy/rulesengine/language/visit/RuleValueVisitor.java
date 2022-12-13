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
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expression;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
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
