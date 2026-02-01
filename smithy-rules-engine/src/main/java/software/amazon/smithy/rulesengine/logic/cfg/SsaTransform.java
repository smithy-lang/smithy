/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;

/**
 * Transforms a decision tree into Static Single Assignment (SSA) form and orchestrates the pre-BDD optimization
 * pipeline (see transform()).
 *
 * <h2>Why Tree-Level AND CFG-Level Optimization?</h2>
 *
 * <p>Tree-level transforms provide the bulk of optimization, but {@link CfgBuilder} performs additional
 * consolidation via {@code consolidateIsSetWithBinding}. This catches cross-branch patterns that tree-level
 * transforms cannot see.
 *
 * <p>Specifically, when one branch contains {@code not(isSet(f(x)))} and another branch contains
 * {@code v = f(x)}, tree-level transforms can't consolidate them because:
 * <ul>
 *   <li>{@link SyntheticBindingTransform} doesn't see the inner {@code isSet} - it checks the outer function
 *       which is {@code Not}, not {@code IsSet}</li>
 *   <li>Tree transforms don't have visibility across sibling branches</li>
 * </ul>
 *
 * <p>During CFG construction, {@code CfgBuilder#isNegationWrapper} unwraps the {@code Not} to get
 * {@code isSet(f(x))}, and {@code consolidateIsSetWithBinding} can then consolidate it with the existing
 * binding from the other branch using its global {@code functionBindings} map.
 *
 * <h2>SSA Renaming</h2>
 *
 * <p>The SSA portion ensures each variable is assigned exactly once by renaming variables when they are
 * reassigned in different parts of the tree. For example, if variable "x" is assigned in multiple branches
 * with different expressions, they become "x_ssa_1", "x_ssa_2", etc. Without this, BDD compilation would
 * incorrectly share nodes for divergent paths that happen to use the same variable name.
 *
 * <p>SSA renaming is only applied when reassignment uses different arguments than previously seen assignments.
 *
 * @see CfgBuilder#createConditionReference for the CFG-level consolidation that catches cross-branch patterns
 */
final class SsaTransform extends TreeMapper {

    private final Deque<Map<String, String>> scopeStack = new ArrayDeque<>();
    private final VariableAnalysis analysis;

    // Maps SSA name -> the rewritten expression string that was first assigned to it.
    // Used to detect when same-text expressions diverge after reference rewriting.
    private final Map<String, String> ssaNameToRewrittenExpr = new HashMap<>();

    // Counter for generating unique SSA names when expressions diverge
    private final Map<String, Integer> ssaCounters = new HashMap<>();

    private SsaTransform(VariableAnalysis analysis) {
        this.analysis = analysis;
        // Seed initial scope with input parameters.
        Map<String, String> initialScope = new HashMap<>();
        for (String param : analysis.getInputParams()) {
            initialScope.put(param, param);
        }
        scopeStack.push(initialScope);
    }

    static EndpointRuleSet transform(EndpointRuleSet ruleSet) {
        // Collapse isSet(X) + booleanEquals(X, true/false) into coalesced checks
        ruleSet = IsSetBooleanCoalesceTransform.transform(ruleSet);
        // Assign synthetic bindings to enable variable consolidation
        ruleSet = SyntheticBindingTransform.transform(ruleSet);
        // Consolidate variables and eliminate redundant bindings
        ruleSet = VariableConsolidationTransform.transform(ruleSet);
        // Remove bindings that are never used (before coalescing inlines them)
        ruleSet = DeadStoreEliminationTransform.transform(ruleSet);
        // Coalesces bind-then-use patterns to reduce condition count and branching.
        ruleSet = CoalesceTransform.transform(ruleSet);
        // Do an SSA transform so divergent paths in the BDD remain logically divergent and thereby correct.
        return new SsaTransform(VariableAnalysis.analyze(ruleSet)).endpointRuleSet(ruleSet);
    }

    @Override
    public Rule rule(Rule r) {
        scopeStack.push(new HashMap<>(peekScope()));
        try {
            return super.rule(r);
        } finally {
            scopeStack.pop();
        }
    }

    @Override
    public Condition condition(Rule rule, Condition condition) {
        LibraryFunction fn = condition.getFunction();

        String uniqueBindingName = null;
        boolean needsUniqueBinding = false;
        String varName = null;

        if (condition.getResult().isPresent()) {
            varName = condition.getResult().get().toString();

            // Check if this variable needs SSA renaming (multiple bindings with different
            // expressions, or transitive dependency on another SSA-renamed variable)
            if (analysis.needsSsaRenaming(varName)) {
                Map<String, String> expressionMap = analysis.getExpressionMappings().get(varName);
                if (expressionMap != null) {
                    uniqueBindingName = expressionMap.get(fn.toString());
                    needsUniqueBinding = uniqueBindingName != null && !uniqueBindingName.equals(varName);
                }
            }
        }

        if (doesNotNeedRewriting(fn.getReferences()) && !needsUniqueBinding) {
            return condition;
        }

        LibraryFunction rewrittenFn = libraryFunction(fn);
        boolean fnChanged = rewrittenFn != fn;

        // If we need a unique binding, check if the rewritten expression matches what we've
        // seen before for this SSA name. If not, we need a fresh name.
        if (needsUniqueBinding) {
            String rewrittenExpr = rewrittenFn.toString();
            String previousExpr = ssaNameToRewrittenExpr.get(uniqueBindingName);
            if (previousExpr == null) {
                ssaNameToRewrittenExpr.put(uniqueBindingName, rewrittenExpr);
            } else if (!previousExpr.equals(rewrittenExpr)) { // note: compares rewritten expression strings
                // Same original expression text but different after rewriting. Needs a fresh name.
                do {
                    int counter = ssaCounters.merge(varName, 1, Integer::sum);
                    uniqueBindingName = varName + "_ssa_" + counter;
                } while (ssaNameToRewrittenExpr.containsKey(uniqueBindingName));
                ssaNameToRewrittenExpr.put(uniqueBindingName, rewrittenExpr);
            }
        }

        if (condition.getResult().isPresent() && uniqueBindingName != null) {
            bindVariable(condition.getResult().get().toString(), uniqueBindingName);
            if (needsUniqueBinding || fnChanged) {
                return condition.toBuilder().fn(rewrittenFn).result(Identifier.of(uniqueBindingName)).build();
            }
        } else if (fnChanged) {
            return condition.toBuilder().fn(rewrittenFn).build();
        }

        return condition;
    }

    Map<String, String> peekScope() {
        return Objects.requireNonNull(scopeStack.peek(), "Scope stack is empty");
    }

    @Override
    public Expression expression(Expression expression) {
        return doesNotNeedRewriting(expression.getReferences()) ? expression : super.expression(expression);
    }

    @Override
    public Reference reference(Reference ref) {
        String originalName = ref.getName().toString();
        String uniqueName = peekScope().getOrDefault(originalName, originalName);
        if (uniqueName.equals(originalName)) {
            return ref;
        }
        return Expression.getReference(Identifier.of(uniqueName));
    }

    private void bindVariable(String oldName, String newName) {
        Map<String, String> scope = peekScope();
        String existing = scope.put(oldName, newName);
        if (existing != null && !existing.equals(oldName)) {
            throw new IllegalStateException("Cannot shadow variable: " + oldName + ", conflicts with " + existing);
        }
    }

    private boolean doesNotNeedRewriting(Set<String> references) {
        Map<String, String> scope = peekScope();
        for (String ref : references) {
            if (!ref.equals(scope.getOrDefault(ref, ref))) {
                return false;
            }
        }
        return true;
    }
}
