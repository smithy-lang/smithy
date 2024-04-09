/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.language.syntax.rule;

import static software.amazon.smithy.rulesengine.language.error.RuleError.context;

import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.utils.SmithyUnstableApi;
import software.amazon.smithy.utils.StringUtils;

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

    /**
     * Gets the error expression to return when reaching this rule.
     *
     * @return the error expression.
     */
    public Expression getError() {
        return error;
    }

    @Override
    public <T> T accept(RuleValueVisitor<T> visitor) {
        return visitor.visitErrorRule(this.error);
    }

    @Override
    protected Type typecheckValue(Scope<Type> scope) {
        return context("while typechecking the error", error, () -> error.typeCheck(scope).expectStringType());
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
}
