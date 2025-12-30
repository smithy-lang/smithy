/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;

/**
 * Consolidates variable bindings for identical expressions.
 *
 * <p>This transform performs two optimizations: elimination and consolidation.
 *
 * <p>Elimination: If an expression is already bound in a parent scope, child bindings are removed.
 * <pre>
 *     url = parseURL(Endpoint)
 *     _synthetic = parseURL(Endpoint) // eliminated, references rewritten to 'url'
 * </pre>
 *
 * <p>Consolidation: If the same expression appears in sibling scopes, all bindings use the first name.
 * <pre>
 *     branch1: url = parseURL(Endpoint)
 *     branch2: parsed = parseURL(Endpoint) // renamed to 'url'
 * </pre>
 *
 * <p>The transform avoids renaming when it would cause variable shadowing.
 *
 * @see SyntheticBindingTransform
 * @see DeadStoreEliminationTransform
 */
final class VariableConsolidationTransform extends TreeMapper {
    private static final Logger LOGGER = Logger.getLogger(VariableConsolidationTransform.class.getName());

    // Global map of canonical expressions to their first variable name seen
    private final Map<String, String> globalExpressionToVar = new HashMap<>();

    // Maps old variable names to new canonical names for rewriting references
    private final Map<String, String> variableRenameMap = new HashMap<>();

    // Tracks conditions to eliminate (using identity for exact instance matching)
    private final Set<Condition> conditionsToEliminate = Collections.newSetFromMap(new IdentityHashMap<>());

    // Tracks all variables defined at each rule to check for conflicts
    private final Map<Rule, Set<String>> ruleDefinedVars = new IdentityHashMap<>();

    private int consolidatedCount = 0;
    private int eliminatedCount = 0;
    private int skippedDueToShadowing = 0;

    public static EndpointRuleSet transform(EndpointRuleSet ruleSet) {
        VariableConsolidationTransform t = new VariableConsolidationTransform();

        // Pass 1: Collect all variable definitions per rule
        for (Rule rule : ruleSet.getRules()) {
            t.collectDefinitions(rule);
        }

        // Pass 2: Discover bindings to consolidate/eliminate
        for (Rule rule : ruleSet.getRules()) {
            t.discoverBindings(rule, new HashMap<>(), new HashSet<>());
        }

        LOGGER.info(String.format("Variable consolidation: %d consolidated, %d eliminated, %d skipped due to shadowing",
                t.consolidatedCount,
                t.eliminatedCount,
                t.skippedDueToShadowing));

        // Pass 3: Transform using TreeMapper
        return t.endpointRuleSet(ruleSet);
    }

    private void collectDefinitions(Rule rule) {
        Set<String> definedVars = new HashSet<>();
        for (Condition condition : rule.getConditions()) {
            condition.getResult().ifPresent(id -> definedVars.add(id.toString()));
        }
        ruleDefinedVars.put(rule, definedVars);

        if (rule instanceof TreeRule) {
            for (Rule nested : ((TreeRule) rule).getRules()) {
                collectDefinitions(nested);
            }
        }
    }

    private void discoverBindings(
            Rule rule,
            Map<String, String> parentBindings,
            Set<String> ancestorVars
    ) {
        Map<String, String> currentBindings = new HashMap<>(parentBindings);
        Set<String> visibleAncestorVars = new HashSet<>(ancestorVars);

        for (Condition condition : rule.getConditions()) {
            if (!condition.getResult().isPresent()) {
                continue;
            }

            String varName = condition.getResult().get().toString();
            String canonical = condition.getFunction().canonicalize().toString();

            // Check if already bound in parent scope
            String parentVar = parentBindings.get(canonical);
            if (parentVar != null) {
                variableRenameMap.put(varName, parentVar);
                conditionsToEliminate.add(condition);
                eliminatedCount++;
                LOGGER.fine(() -> String.format("Eliminating redundant binding: '%s' -> '%s' for: %s",
                        varName, parentVar, canonical));
            } else {
                currentBindings.put(canonical, varName);
                visibleAncestorVars.add(varName);

                // Check for global consolidation opportunity
                String globalVar = globalExpressionToVar.get(canonical);
                if (globalVar != null && !globalVar.equals(varName)) {
                    boolean wouldShadow = wouldCauseShadowing(globalVar, rule, ancestorVars);
                    if (!wouldShadow) {
                        // No shadowing - safe to rename the binding
                        variableRenameMap.put(varName, globalVar);
                        consolidatedCount++;
                        LOGGER.fine(() -> String.format("Consolidating '%s' -> '%s' for: %s",
                                varName, globalVar, canonical));
                    } else {
                        skippedDueToShadowing++;
                        LOGGER.info(() -> String.format("Shadowing skip: '%s' -> '%s' for expr: %s",
                                varName, globalVar, canonical));
                    }
                } else if (globalVar == null) {
                    globalExpressionToVar.put(canonical, varName);
                }
            }
        }

        if (rule instanceof TreeRule) {
            for (Rule nested : ((TreeRule) rule).getRules()) {
                discoverBindings(nested, currentBindings, visibleAncestorVars);
            }
        }
    }

    private boolean wouldCauseShadowing(String varName, Rule currentRule, Set<String> ancestorVars) {
        if (ancestorVars.contains(varName)) {
            return true;
        }

        // Check if any descendant rule defines this variable
        return wouldShadowInDescendants(varName, currentRule);
    }

    private boolean wouldShadowInDescendants(String varName, Rule rule) {
        if (rule instanceof TreeRule) {
            for (Rule nested : ((TreeRule) rule).getRules()) {
                Set<String> nestedVars = ruleDefinedVars.get(nested);
                if (nestedVars != null && nestedVars.contains(varName)) {
                    return true;
                }
                if (wouldShadowInDescendants(varName, nested)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Condition condition(Rule rule, Condition condition) {
        // Eliminate redundant conditions
        if (conditionsToEliminate.contains(condition)) {
            return null;
        }

        LibraryFunction fn = condition.getFunction();
        LibraryFunction rewrittenFn = libraryFunction(fn);

        // Check if binding needs renaming
        if (condition.getResult().isPresent()) {
            String varName = condition.getResult().get().toString();
            String canonicalName = variableRenameMap.get(varName);

            if (canonicalName != null) {
                return Condition.builder()
                        .fn(rewrittenFn)
                        .result(Identifier.of(canonicalName))
                        .build();
            }
        }

        // Only rebuild if function changed
        if (rewrittenFn != fn) {
            return condition.toBuilder().fn(rewrittenFn).build();
        }

        return condition;
    }

    @Override
    public Expression expression(Expression expression) {
        // Short-circuit if no references need rewriting
        for (String ref : expression.getReferences()) {
            if (variableRenameMap.containsKey(ref)) {
                return super.expression(expression);
            }
        }
        return expression;
    }

    @Override
    public Reference reference(Reference ref) {
        String canonicalName = variableRenameMap.get(ref.getName().toString());
        if (canonicalName != null) {
            return Expression.getReference(Identifier.of(canonicalName));
        }
        return ref;
    }
}
