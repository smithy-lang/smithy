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
 * <p>The builder performs simple hash-consing during top-down construction, deduplicating nodes when the same node
 * would be created multiple times in the same context.
 */
public final class CfgBuilder {
    final EndpointRuleSet ruleSet;

    // Simple hash-consing for nodes created in the same context
    private final Map<NodeSignature, CfgNode> nodeCache = new HashMap<>();

    // Condition and result canonicalization
    private final Map<Condition, ConditionReference> conditionToReference = new HashMap<>();
    private final Map<Rule, Rule> resultCache = new HashMap<>();
    private final Map<Rule, ResultNode> resultNodeCache = new HashMap<>();

    public CfgBuilder(EndpointRuleSet ruleSet) {
        // Disambiguate conditions and references so variable names are globally unique.
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
     *
     * @param condition   the condition to evaluate
     * @param trueBranch  the node to evaluate when the condition is true
     * @param falseBranch the node to evaluate when the condition is false
     * @return a condition node (possibly cached)
     */
    public CfgNode createCondition(Condition condition, CfgNode trueBranch, CfgNode falseBranch) {
        return createCondition(createConditionReference(condition), trueBranch, falseBranch);
    }

    /**
     * Creates a condition node, reusing existing nodes when possible.
     *
     * @param condRef the condition reference to evaluate
     * @param trueBranch the node to evaluate when the condition is true
     * @param falseBranch the node to evaluate when the condition is false
     * @return a condition node (possibly cached)
     */
    public CfgNode createCondition(ConditionReference condRef, CfgNode trueBranch, CfgNode falseBranch) {
        NodeSignature signature = new NodeSignature(condRef, trueBranch, falseBranch);
        return nodeCache.computeIfAbsent(signature, key -> new ConditionNode(condRef, trueBranch, falseBranch));
    }

    /**
     * Creates a result node representing a terminal rule evaluation.
     *
     * @param rule the result rule (endpoint or error)
     * @return a result node (cached if identical rule already seen)
     */
    public CfgNode createResult(Rule rule) {
        Rule canonical = rule.withConditions(Collections.emptyList());
        Rule interned = resultCache.computeIfAbsent(canonical, k -> k);
        return resultNodeCache.computeIfAbsent(interned, ResultNode::new);
    }

    /**
     * Creates a canonical condition reference, handling negation and deduplication.
     */
    public ConditionReference createConditionReference(Condition condition) {
        // Check cache first
        ConditionReference cached = conditionToReference.get(condition);
        if (cached != null) {
            return cached;
        }

        // Check if it's a negation
        boolean negated = false;
        Condition canonical = condition;

        if (isNegationWrapper(condition)) {
            negated = true;
            canonical = unwrapNegation(condition);

            // Check if we already have the non-negated version
            ConditionReference existing = conditionToReference.get(canonical);
            if (existing != null) {
                // Reuse the existing Condition, just negate the reference
                ConditionReference negatedReference = existing.negate();
                conditionToReference.put(condition, negatedReference);
                return negatedReference;
            }
        }

        // Canonicalize for commutative operations
        canonical = canonical.canonicalize();

        // Canonicalize boolean equals
        Condition beforeBooleanCanon = canonical;
        canonical = canonicalizeBooleanEquals(canonical);

        if (!canonical.equals(beforeBooleanCanon)) {
            negated = !negated;
        }

        // Create the reference (possibly negated)
        ConditionReference reference = new ConditionReference(canonical, negated);

        // Cache the reference under the original key
        conditionToReference.put(condition, reference);

        // Also cache under the canonical form if different
        if (!negated && !condition.equals(canonical)) {
            conditionToReference.put(canonical, reference);
        }

        return reference;
    }

    private Condition canonicalizeBooleanEquals(Condition condition) {
        if (!(condition.getFunction() instanceof BooleanEquals)) {
            return condition;
        }

        List<Expression> args = condition.getFunction().getArguments();

        // After commutative canonicalization, if there's a reference, it's in position 0
        if (args.get(0) instanceof Reference && args.get(1) instanceof Literal) {
            Reference ref = (Reference) args.get(0);
            Boolean literalValue = ((Literal) args.get(1)).asBooleanLiteral().orElse(null);

            if (literalValue != null && !literalValue && ruleSet != null) {
                String varName = ref.getName().toString();
                Optional<Parameter> param = ruleSet.getParameters().get(Identifier.of(varName));
                if (param.isPresent() && param.get().getDefault().isPresent()) {
                    // Convert booleanEquals(var, false) to booleanEquals(var, true)
                    return condition.toBuilder().fn(BooleanEquals.ofExpressions(ref, true)).build();
                }
            }
        }

        return condition;
    }

    private static boolean isNegationWrapper(Condition condition) {
        if (!(condition.getFunction() instanceof Not)) {
            return false;
        } else if (condition.getResult().isPresent()) {
            return false;
        } else {
            return condition.getFunction().getArguments().get(0) instanceof LibraryFunction;
        }
    }

    private static Condition unwrapNegation(Condition negatedCondition) {
        return negatedCondition.toBuilder()
                .fn((LibraryFunction) negatedCondition.getFunction().getArguments().get(0))
                .build();
    }

    // Signature for node deduplication during construction.
    private static final class NodeSignature {
        private final ConditionReference condition;
        private final CfgNode trueBranch;
        private final CfgNode falseBranch;
        private final int hashCode;

        NodeSignature(ConditionReference condition, CfgNode trueBranch, CfgNode falseBranch) {
            this.condition = condition;
            this.trueBranch = trueBranch;
            this.falseBranch = falseBranch;
            // Use identity hash for branches.
            this.hashCode = Objects.hash(
                    condition,
                    System.identityHashCode(trueBranch),
                    System.identityHashCode(falseBranch));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (!(o instanceof NodeSignature)) {
                return false;
            }
            NodeSignature that = (NodeSignature) o;
            // Reference equality for children
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
