/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.BooleanEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Not;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.logic.ConditionReference;

/**
 * Builder for constructing Control Flow Graphs with node deduplication.
 *
 * <p>This builder performs hash-consing during construction to share identical
 * subtrees and prevent exponential growth.
 */
public final class CfgBuilder {

    final EndpointRuleSet ruleSet;

    // Node deduplication
    private final Map<NodeSignature, CfgNode> nodeCache = new HashMap<>();

    // Condition and result canonicalization
    private final Map<Condition, ConditionReference> conditionToReference = new HashMap<>();
    private final Map<Rule, Rule> resultCache = new HashMap<>();
    private final Map<Rule, ResultNode> resultNodeCache = new HashMap<>();

    public CfgBuilder(EndpointRuleSet ruleSet) {
        // Apply SSA transform to ensure globally unique variable names
        this.ruleSet = SsaTransform.transform(ruleSet);
    }

    /**
     * Build the CFG with the given root node.
     *
     * @param root Root node to use for the built CFG.
     * @return the built CFG.
     */
    public Cfg build(CfgNode root) {
        return new Cfg(ruleSet, Objects.requireNonNull(root));
    }

    /**
     * Creates a condition node, reusing existing nodes when possible.
     */
    public CfgNode createCondition(Condition condition, CfgNode trueBranch, CfgNode falseBranch) {
        return createCondition(createConditionReference(condition), trueBranch, falseBranch);
    }

    /**
     * Creates a condition node, reusing existing nodes when possible.
     */
    public CfgNode createCondition(ConditionReference condRef, CfgNode trueBranch, CfgNode falseBranch) {
        NodeSignature signature = new NodeSignature(condRef, trueBranch, falseBranch);
        return nodeCache.computeIfAbsent(signature, key -> new ConditionNode(condRef, trueBranch, falseBranch));
    }

    /**
     * Creates a result node representing a terminal rule evaluation.
     */
    public CfgNode createResult(Rule rule) {
        // Intern the result
        Rule interned = intern(rule);

        // Regular result node
        return resultNodeCache.computeIfAbsent(interned, ResultNode::new);
    }

    /**
     * Creates a canonical condition reference, handling negation and deduplication.
     */
    public ConditionReference createConditionReference(Condition condition) {
        ConditionReference cached = conditionToReference.get(condition);
        if (cached != null) {
            return cached;
        }

        boolean negated = false;
        Condition canonical = condition;

        if (isNegationWrapper(condition)) {
            negated = true;
            canonical = unwrapNegation(condition);

            ConditionReference existing = conditionToReference.get(canonical);
            if (existing != null) {
                ConditionReference negatedReference = existing.negate();
                conditionToReference.put(condition, negatedReference);
                return negatedReference;
            }
        }

        canonical = canonical.canonicalize();

        Condition beforeBooleanCanon = canonical;
        canonical = canonicalizeBooleanEquals(canonical);

        if (!canonical.equals(beforeBooleanCanon)) {
            negated = !negated;
        }

        ConditionReference reference = new ConditionReference(canonical, negated);
        conditionToReference.put(condition, reference);

        if (!negated && !condition.equals(canonical)) {
            conditionToReference.put(canonical, reference);
        }

        return reference;
    }

    private Rule intern(Rule rule) {
        return resultCache.computeIfAbsent(canonicalizeResult(rule), k -> k);
    }

    private Rule canonicalizeResult(Rule rule) {
        return rule == null ? null : rule.withConditions(Collections.emptyList());
    }

    private Condition canonicalizeBooleanEquals(Condition condition) {
        if (!(condition.getFunction() instanceof BooleanEquals)) {
            return condition;
        }

        List<Expression> args = condition.getFunction().getArguments();
        if (args.size() != 2 || !(args.get(0) instanceof Reference) || !(args.get(1) instanceof Literal)) {
            return condition;
        }

        Reference ref = (Reference) args.get(0);
        Boolean literalValue = ((Literal) args.get(1)).asBooleanLiteral().orElse(null);

        if (literalValue != null && !literalValue && ruleSet != null) {
            String varName = ref.getName().toString();
            Optional<Parameter> param = ruleSet.getParameters().get(Identifier.of(varName));
            if (param.isPresent() && param.get().getDefault().isPresent()) {
                return condition.toBuilder().fn(BooleanEquals.ofExpressions(ref, true)).build();
            }
        }

        return condition;
    }

    private static boolean isNegationWrapper(Condition condition) {
        return condition.getFunction() instanceof Not
                && !condition.getResult().isPresent()
                && condition.getFunction().getArguments().get(0) instanceof LibraryFunction;
    }

    private static Condition unwrapNegation(Condition negatedCondition) {
        return negatedCondition.toBuilder()
                .fn((LibraryFunction) negatedCondition.getFunction().getArguments().get(0))
                .build();
    }

    /**
     * Signature for node deduplication during construction.
     */
    private static final class NodeSignature {
        private final ConditionReference condition;
        private final CfgNode trueBranch;
        private final CfgNode falseBranch;
        private final int hashCode;

        NodeSignature(ConditionReference condition, CfgNode trueBranch, CfgNode falseBranch) {
            this.condition = condition;
            this.trueBranch = trueBranch;
            this.falseBranch = falseBranch;
            this.hashCode = Objects.hash(
                    condition,
                    System.identityHashCode(trueBranch),
                    System.identityHashCode(falseBranch));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof NodeSignature)) {
                return false;
            }
            NodeSignature that = (NodeSignature) o;
            return Objects.equals(condition, that.condition)
                    && trueBranch == that.trueBranch
                    && falseBranch == that.falseBranch;
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
