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

package software.amazon.smithy.rulesengine.language.lang.rule;

import static software.amazon.smithy.rulesengine.language.error.RuleError.ctx;

import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.model.SourceException;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.util.StringUtils;
import software.amazon.smithy.rulesengine.language.visit.RuleValueVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class TreeRule extends Rule {
    private final List<Rule> rules;

    protected TreeRule(Builder builder, List<Rule> rules) {
        super(builder);
        this.rules = rules;
    }

    boolean isExhaustive() {
        return this.rules.get(this.rules.size() - 1).getConditions().isEmpty();
    }

    public List<Rule> getRules() {
        return rules;
    }

    @Override
    public <T> T accept(RuleValueVisitor<T> visitor) {
        return visitor.visitTreeRule(this.rules);
    }

    @Override
    protected Type typecheckValue(Scope<Type> scope) {
        if (rules.isEmpty()) {
            throw new SourceException("Tree rule contains no rules!", this.getSourceLocation());
        }
        for (Rule rule : rules) {
            ctx(
                    "while checking nested rule in tree rule",
                    () -> scope.inScope(() -> rule.typecheck(scope)));
        }
        return Type.endpoint();
    }

    @Override
    void withValueNode(ObjectNode.Builder builder) {
        builder.withMember(TYPE, TREE).withMember(
                "rules",
                new ArrayNode(rules.stream().map(Rule::toNode).collect(Collectors.toList()), this.getSourceLocation()));
    }

    @Override
    public String toString() {
        return super.toString()
               + StringUtils.indent(rules.stream().map(Rule::toString).collect(Collectors.joining("\n")), 2);
    }
}
