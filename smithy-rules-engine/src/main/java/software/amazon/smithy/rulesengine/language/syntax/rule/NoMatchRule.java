/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.syntax.rule;

import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.evaluation.Scope;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.utils.SmithyUnstableApi;

/**
 * Sentinel rule for "no match" results.
 */
@SmithyUnstableApi
public final class NoMatchRule extends Rule {

    public static final NoMatchRule INSTANCE = new NoMatchRule();

    private NoMatchRule() {
        super(Rule.builder());
    }

    @Override
    public <T> T accept(RuleValueVisitor<T> visitor) {
        throw new UnsupportedOperationException("NO_MATCH is a sentinel");
    }

    @Override
    protected Type typecheckValue(Scope<Type> scope) {
        return Type.anyType();
    }

    @Override
    protected void withValueNode(ObjectNode.Builder builder) {
        // nothing
    }

    @Override
    public String toString() {
        return "NO_MATCH";
    }
}
