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

import java.util.ArrayList;
import java.util.List;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.error.RuleError;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.StringUtils;

@SmithyUnstableApi
public final class TreeRule extends Rule {
    private final List<Rule> rules;

    TreeRule(Builder builder, List<Rule> rules) {
        super(builder);
        this.rules = rules;
    }

    public List<Rule> getRules() {
        return rules;
    }

    @Override
    public <T> T accept(RuleValueVisitor<T> visitor) {
        return visitor.visitTreeRule(rules);
    }

    @Override
    protected Type typecheckValue(Scope<Type> scope) {
        if (rules.isEmpty()) {
            throw new SourceException("Tree rule contains no rules.", getSourceLocation());
        }
        for (Rule rule : rules) {
            RuleError.context("while checking nested rule in tree rule",
                    () -> scope.inScope(() -> rule.typeCheck(scope)));
        }
        return Type.endpointType();
    }

    @Override
    void withValueNode(ObjectNode.Builder builder) {
        ArrayNode.Builder rulesBuilder = ArrayNode.builder().sourceLocation(getSourceLocation());
        for (Rule rule : rules) {
            rulesBuilder.withValue(rule.toNode());
        }
        builder.withMember("rules", rulesBuilder.build()).withMember(TYPE, TREE);
    }

    @Override
    public String toString() {
        List<String> ruleStrings = new ArrayList<>();
        for (Rule rule : rules) {
            ruleStrings.add(rule.toString());
        }
        return super.toString() + StringUtils.indent(String.join("\n", ruleStrings), 2);
    }
}
