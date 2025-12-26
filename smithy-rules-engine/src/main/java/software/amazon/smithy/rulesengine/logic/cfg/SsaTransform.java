/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;

/**
 * Transforms a decision tree into Static Single Assignment (SSA) form.
 *
 * <p>This transformation ensures that each variable is assigned exactly once by renaming variables when they are
 * reassigned in different parts of the tree. For example, if variable "x" is assigned in multiple branches, they
 * become "x_ssa_1", "x_ssa_2", "x_ssa_3", etc. Without this transform, the BDD compilation would confuse divergent
 * paths that have the same variable name.
 *
 * <p>Note that this transform is only applied when the reassignment is done using different
 * arguments than previously seen assignments of the same variable name.
 */
final class SsaTransform {

    private final Deque<Map<String, String>> scopeStack = new ArrayDeque<>();
    private final Map<Condition, Condition> rewrittenConditions = new IdentityHashMap<>();
    private final Map<Rule, Rule> rewrittenRules = new IdentityHashMap<>();
    private final VariableAnalysis variableAnalysis;
    private final TreeRewriter referenceRewriter;

    private SsaTransform(VariableAnalysis variableAnalysis) {
        scopeStack.push(new HashMap<>());
        this.variableAnalysis = variableAnalysis;
        this.referenceRewriter = new TreeRewriter(this::referenceRewriter, this::needsRewriting);
    }

    private Expression referenceRewriter(Reference ref) {
        String originalName = ref.getName().toString();
        String uniqueName = resolveReference(originalName);
        return Expression.getReference(Identifier.of(uniqueName));
    }

    static EndpointRuleSet transform(EndpointRuleSet ruleSet) {
        // Unwrap isSet(f(x)) to _synthetic = f(x), enabling consolidation with real bindings
        ruleSet = UnwrapIsSetTransform.transform(ruleSet);

        // Assign synthetic bindings to remaining bare function calls
        ruleSet = BareFunctionBindingTransform.transform(ruleSet);

        // Consolidate variables and eliminate redundant bindings
        ruleSet = VariableConsolidationTransform.transform(ruleSet);

        // Remove bindings that are never used (before coalescing inlines them)
        ruleSet = DeadStoreEliminationTransform.transform(ruleSet);

        ruleSet = CoalesceTransform.transform(ruleSet);
        
        VariableAnalysis variableAnalysis = VariableAnalysis.analyze(ruleSet);
        SsaTransform ssaTransform = new SsaTransform(variableAnalysis);

        List<Rule> rewrittenRules = new ArrayList<>(ruleSet.getRules().size());
        for (Rule original : ruleSet.getRules()) {
            rewrittenRules.add(ssaTransform.processRule(original));
        }

        return EndpointRuleSet.builder()
                .parameters(ruleSet.getParameters())
                .rules(rewrittenRules)
                .version(ruleSet.getVersion())
                .build();
    }

    private Rule processRule(Rule rule) {
        enterScope();
        Rule rewrittenRule = rewriteRule(rule);
        exitScope();
        return rewrittenRule;
    }

    private void enterScope() {
        scopeStack.push(new HashMap<>(scopeStack.peek()));
    }

    private void exitScope() {
        if (scopeStack.size() <= 1) {
            throw new IllegalStateException("Cannot exit global scope");
        }
        scopeStack.pop();
    }

    private Condition rewriteCondition(Condition condition) {
        boolean hasBinding = condition.getResult().isPresent();

        if (!hasBinding) {
            Condition cached = rewrittenConditions.get(condition);
            if (cached != null) {
                return cached;
            }
        }

        LibraryFunction fn = condition.getFunction();
        Set<String> rewritableRefs = filterOutInputParameters(fn.getReferences());

        String uniqueBindingName = null;
        boolean needsUniqueBinding = false;
        if (hasBinding) {
            String varName = condition.getResult().get().toString();

            // Only need SSA rename if variable has multiple bindings
            if (variableAnalysis.hasMultipleBindings(varName)) {
                Map<String, String> expressionMap = variableAnalysis.getExpressionMappings().get(varName);
                if (expressionMap != null) {
                    uniqueBindingName = expressionMap.get(fn.toString());
                    needsUniqueBinding = uniqueBindingName != null && !uniqueBindingName.equals(varName);
                }
            }
        }

        if (!needsRewriting(rewritableRefs) && !needsUniqueBinding) {
            if (!hasBinding) {
                rewrittenConditions.put(condition, condition);
            }
            return condition;
        }

        LibraryFunction rewrittenExpr = (LibraryFunction) referenceRewriter.rewrite(fn);
        boolean exprChanged = rewrittenExpr != fn;

        Condition rewritten;
        if (hasBinding && uniqueBindingName != null) {
            scopeStack.peek().put(condition.getResult().get().toString(), uniqueBindingName);
            if (needsUniqueBinding || exprChanged) {
                rewritten = condition.toBuilder().fn(rewrittenExpr).result(Identifier.of(uniqueBindingName)).build();
            } else {
                rewritten = condition;
            }
        } else if (exprChanged) {
            rewritten = condition.toBuilder().fn(rewrittenExpr).build();
        } else {
            rewritten = condition;
        }

        if (!hasBinding) {
            rewrittenConditions.put(condition, rewritten);
        }

        return rewritten;
    }

    private Set<String> filterOutInputParameters(Set<String> references) {
        if (references.isEmpty() || variableAnalysis.getInputParams().isEmpty()) {
            return references;
        }

        Set<String> filtered = new HashSet<>(references);
        filtered.removeAll(variableAnalysis.getInputParams());
        return filtered;
    }

    private boolean needsRewriting(Set<String> references) {
        if (references.isEmpty()) {
            return false;
        }

        Map<String, String> currentScope = scopeStack.peek();
        for (String ref : references) {
            String mapped = currentScope.get(ref);
            if (mapped != null && !mapped.equals(ref)) {
                return true;
            }
        }
        return false;
    }

    private boolean needsRewriting(Expression expression) {
        return needsRewriting(filterOutInputParameters(expression.getReferences()));
    }

    private Rule rewriteRule(Rule rule) {
        Rule cached = rewrittenRules.get(rule);
        if (cached != null) {
            return cached;
        }

        List<Condition> rewrittenConditions = rewriteConditions(rule.getConditions());
        boolean conditionsChanged = !rewrittenConditions.equals(rule.getConditions());

        Rule result;
        if (rule instanceof EndpointRule) {
            result = rewriteEndpointRule((EndpointRule) rule, rewrittenConditions, conditionsChanged);
        } else if (rule instanceof ErrorRule) {
            result = rewriteErrorRule((ErrorRule) rule, rewrittenConditions, conditionsChanged);
        } else if (rule instanceof TreeRule) {
            result = rewriteTreeRule((TreeRule) rule, rewrittenConditions, conditionsChanged);
        } else if (conditionsChanged) {
            throw new UnsupportedOperationException("Cannot change rule: " + rule);
        } else {
            result = rule;
        }

        rewrittenRules.put(rule, result);
        return result;
    }

    private List<Condition> rewriteConditions(List<Condition> conditions) {
        List<Condition> rewritten = new ArrayList<>(conditions.size());
        for (Condition condition : conditions) {
            rewritten.add(rewriteCondition(condition));
        }
        return rewritten;
    }

    private Rule rewriteEndpointRule(
            EndpointRule rule,
            List<Condition> rewrittenConditions,
            boolean conditionsChanged
    ) {
        Endpoint rewrittenEndpoint = referenceRewriter.rewriteEndpoint(rule.getEndpoint());

        if (conditionsChanged || rewrittenEndpoint != rule.getEndpoint()) {
            return EndpointRule.builder()
                    .description(rule.getDocumentation().orElse(null))
                    .conditions(rewrittenConditions)
                    .endpoint(rewrittenEndpoint);
        }

        return rule;
    }

    private Rule rewriteErrorRule(ErrorRule rule, List<Condition> rewrittenConditions, boolean conditionsChanged) {
        Expression rewrittenError = referenceRewriter.rewrite(rule.getError());

        if (conditionsChanged || rewrittenError != rule.getError()) {
            return ErrorRule.builder()
                    .description(rule.getDocumentation().orElse(null))
                    .conditions(rewrittenConditions)
                    .error(rewrittenError);
        }

        return rule;
    }

    private Rule rewriteTreeRule(TreeRule rule, List<Condition> rewrittenConditions, boolean conditionsChanged) {
        List<Rule> rewrittenNestedRules = new ArrayList<>();
        boolean nestedChanged = false;

        for (Rule nestedRule : rule.getRules()) {
            enterScope();
            Rule rewritten = rewriteRule(nestedRule);
            rewrittenNestedRules.add(rewritten);
            if (rewritten != nestedRule) {
                nestedChanged = true;
            }
            exitScope();
        }

        if (conditionsChanged || nestedChanged) {
            return TreeRule.builder()
                    .description(rule.getDocumentation().orElse(null))
                    .conditions(rewrittenConditions)
                    .treeRule(rewrittenNestedRules);
        }

        return rule;
    }

    private String resolveReference(String originalName) {
        // Input parameters are never rewritten
        return variableAnalysis.getInputParams().contains(originalName)
                ? originalName
                : scopeStack.peek().getOrDefault(originalName, originalName);
    }
}
