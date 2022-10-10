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

import static software.amazon.smithy.rulesengine.language.error.RuleError.context;

import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.Type;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expression;
import software.amazon.smithy.rulesengine.language.util.StringUtils;
import software.amazon.smithy.rulesengine.language.visit.RuleValueVisitor;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * A rule-set rule that is used to indicate an error in evaluation.
 */
@SmithyUnstableApi
public final class ErrorRule extends Rule {
    private final Expression error;

    public ErrorRule(Rule.Builder builder, Expression error) {
        super(builder);
        this.error = error;
    }

    @Override
    public <T> T accept(RuleValueVisitor<T> visitor) {
        return visitor.visitErrorRule(this.error);
    }

    @Override
    protected Type typecheckValue(Scope<Type> scope) {
        return context("while typechecking the error", error, () -> error.typeCheck(scope).expectString());
    }

    @Override
    public Node toNode() {
        ObjectNode.Builder on = super.toNode().expectObjectNode().toBuilder();
        on.withMember(Rule.ERROR, error);
        return on.build();
    }

    @Override
    void withValueNode(ObjectNode.Builder builder) {
        builder.withMember("error", error.toNode()).withMember(TYPE, ERROR);
    }

    @Override
    public String toString() {
        return super.toString()
               + StringUtils.indent(String.format("error(%s)", error), 2);
    }

    public Expression getError() {
        return error;
    }
}
