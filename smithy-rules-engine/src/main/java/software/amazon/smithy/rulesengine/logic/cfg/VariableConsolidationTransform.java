/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;

/**
 * Consolidates variable names for identical expressions and eliminates redundant bindings.
 *
 * <p>This transform identifies conditions that compute the same expression but assign
 * the result to different variable names, and either consolidates them to use the same
 * name or eliminates redundant bindings when the same expression is already bound in
 * an ancestor scope.
 */
final class VariableConsolidationTransform {
    private static final Logger LOGGER = Logger.getLogger(VariableConsolidationTransform.class.getName());

    // Global map of canonical expressions to their first variable name seen
    private final Map<String, String> globalExpressionToVar = new HashMap<>();

    // Maps old variable names to new canonical names for rewriting references
    private final Map<String, String> variableRenameMap = new HashMap<>();

    // Tracks conditions to eliminate (by their path in the tree)
    private final Set<String> conditionsToEliminate = new HashSet<>();

    // Tracks all variables defined at each scope level to check for conflicts
    private final Map<String, Set<String>> scopeDefinedVars = new HashMap<>();

    private int consolidatedCount = 0;
    private int eliminatedCount = 0;
    private int skippedDueToShadowing = 0;

    public static EndpointRuleSet transform(EndpointRuleSet ruleSet) {
        VariableConsolidationTransform transform = new VariableConsolidationTransform();
        return transform.consolidate(ruleSet);
    }

    private EndpointRuleSet consolidate(EndpointRuleSet ruleSet) {
        LOGGER.info("Starting variable consolidation transform");

        for (int i = 0; i < ruleSet.getRules().size(); i++) {
            collectDefinitions(ruleSet.getRules().get(i), "rule[" + i + "]");
        }

        for (int i = 0; i < ruleSet.getRules().size(); i++) {
            discoverBindingsInRule(ruleSet.getRules().get(i), "rule[" + i + "]", new HashMap<>(), new HashSet<>());
        }

        List<Rule> transformedRules = new ArrayList<>();
        for (int i = 0; i < ruleSet.getRules().size(); i++) {
            transformedRules.add(transformRule(ruleSet.getRules().get(i), "rule[" + i + "]"));
        }

        LOGGER.info(String.format("Variable consolidation: %d consolidated, %d eliminated, %d skipped due to shadowing",
                consolidatedCount,
                eliminatedCount,
                skippedDueToShadowing));

        return EndpointRuleSet.builder()
                .parameters(ruleSet.getParameters())
                .rules(transformedRules)
                .version(ruleSet.getVersion())
                .build();
    }

    private void collectDefinitions(Rule rule, String path) {
        Set<String> definedVars = new HashSet<>();

        // Collect all variables defined at this scope level
        for (Condition condition : rule.getConditions()) {
            if (condition.getResult().isPresent()) {
                definedVars.add(condition.getResult().get().toString());
            }
        }

        scopeDefinedVars.put(path, definedVars);

        if (rule instanceof TreeRule) {
            TreeRule treeRule = (TreeRule) rule;
            for (int i = 0; i < treeRule.getRules().size(); i++) {
                collectDefinitions(treeRule.getRules().get(i), path + "/tree/rule[" + i + "]");
            }
        }
    }

    private void discoverBindingsInRule(
            Rule rule,
            String path,
            Map<String, String> parentBindings,
            Set<String> ancestorVars
    ) {
        // Track bindings at current scope (inherits parent bindings)
        Map<String, String> currentBindings = new HashMap<>(parentBindings);
        // Track all variables visible from ancestors (for shadowing check)
        Set<String> visibleAncestorVars = new HashSet<>(ancestorVars);

        for (int i = 0; i < rule.getConditions().size(); i++) {
            Condition condition = rule.getConditions().get(i);
            String condPath = path + "/cond[" + i + "]";

            if (condition.getResult().isPresent()) {
                String varName = condition.getResult().get().toString();
                LibraryFunction fn = condition.getFunction();
                String canonical = fn.canonicalize().toString();

                // Check if this expression is already bound in parent scope
                String parentVar = parentBindings.get(canonical);
                if (parentVar != null) {
                    // Found duplicate in parent, eliminate this binding
                    variableRenameMap.put(varName, parentVar);
                    conditionsToEliminate.add(condPath);
                    eliminatedCount++;
                    LOGGER.info(String.format("Eliminating redundant binding at %s: '%s' -> '%s' for: %s",
                            condPath,
                            varName,
                            parentVar,
                            canonical));
                } else {
                    // Not bound in parent, add to current scope
                    currentBindings.put(canonical, varName);
                    visibleAncestorVars.add(varName);

                    // Check for global consolidation opportunity
                    String globalVar = globalExpressionToVar.get(canonical);
                    if (globalVar != null && !globalVar.equals(varName)) {
                        // Same expression elsewhere with different name - consolidate if no shadowing
                        if (!wouldCauseShadowing(globalVar, path, ancestorVars)) {
                            variableRenameMap.put(varName, globalVar);
                            consolidatedCount++;
                            LOGGER.info(String.format("Consolidating '%s' -> '%s' for: %s",
                                    varName,
                                    globalVar,
                                    canonical));
                        } else {
                            skippedDueToShadowing++;
                            LOGGER.fine(String.format("Cannot consolidate '%s' -> '%s' (would shadow) for: %s",
                                    varName,
                                    globalVar,
                                    canonical));
                        }
                    } else if (globalVar == null) {
                        // First time seeing this expression globally
                        globalExpressionToVar.put(canonical, varName);
                    }
                }
            }
        }

        if (rule instanceof TreeRule) {
            TreeRule treeRule = (TreeRule) rule;
            for (int i = 0; i < treeRule.getRules().size(); i++) {
                discoverBindingsInRule(
                        treeRule.getRules().get(i),
                        path + "/tree/rule[" + i + "]",
                        currentBindings,
                        visibleAncestorVars);
            }
        }
    }

    /**
     * Checks if two variable names have the same base name.
     * For SSA-style variables like "foo_1" and "foo_2", the base name is "foo".
     * Variables without SSA suffix (like "s3e_fips" and "s3e_ds") are considered
     * to have their full name as the base.
     */
    private boolean hasSameBaseName(String var1, String var2) {
        String base1 = getSsaBaseName(var1);
        String base2 = getSsaBaseName(var2);
        return base1.equals(base2);
    }

    /**
     * Extracts the SSA base name from a variable.
     * If the variable ends with _N (where N is a number), strips the suffix.
     * Otherwise returns the full name.
     */
    private String getSsaBaseName(String varName) {
        int lastUnderscore = varName.lastIndexOf('_');
        if (lastUnderscore > 0 && lastUnderscore < varName.length() - 1) {
            String suffix = varName.substring(lastUnderscore + 1);
            // Check if suffix is all digits
            boolean allDigits = true;
            for (int i = 0; i < suffix.length(); i++) {
                if (!Character.isDigit(suffix.charAt(i))) {
                    allDigits = false;
                    break;
                }
            }
            if (allDigits) {
                return varName.substring(0, lastUnderscore);
            }
        }
        return varName;
    }

    private boolean wouldCauseShadowing(String varName, String currentPath, Set<String> ancestorVars) {
        // Check if using this variable name would shadow an ancestor variable
        if (ancestorVars.contains(varName)) {
            return true;
        }

        // Check if any child scope already defines this variable
        // (which would be shadowed if we use it here)
        for (Map.Entry<String, Set<String>> entry : scopeDefinedVars.entrySet()) {
            String scopePath = entry.getKey();
            Set<String> scopeVars = entry.getValue();
            // Check if this scope is a descendant of current path
            if (scopePath.startsWith(currentPath + "/") && scopeVars.contains(varName)) {
                return true;
            }
        }

        return false;
    }

    private Rule transformRule(Rule rule, String path) {
        List<Condition> transformedConditions = new ArrayList<>();

        for (int i = 0; i < rule.getConditions().size(); i++) {
            String condPath = path + "/cond[" + i + "]";

            if (conditionsToEliminate.contains(condPath)) {
                // Skip this condition entirely since it's redundant
                continue;
            }

            Condition condition = rule.getConditions().get(i);
            transformedConditions.add(transformCondition(condition));
        }

        if (rule instanceof TreeRule) {
            TreeRule treeRule = (TreeRule) rule;
            return TreeRule.builder()
                    .description(rule.getDocumentation().orElse(null))
                    .conditions(transformedConditions)
                    .treeRule(TreeRewriter.transformNestedRules(treeRule, path, this::transformRule));

        } else if (rule instanceof EndpointRule) {
            EndpointRule endpointRule = (EndpointRule) rule;
            TreeRewriter rewriter = createRewriter();

            return EndpointRule.builder()
                    .description(rule.getDocumentation().orElse(null))
                    .conditions(transformedConditions)
                    .endpoint(rewriter.rewriteEndpoint(endpointRule.getEndpoint()));

        } else if (rule instanceof ErrorRule) {
            ErrorRule errorRule = (ErrorRule) rule;
            TreeRewriter rewriter = createRewriter();

            return ErrorRule.builder()
                    .description(rule.getDocumentation().orElse(null))
                    .conditions(transformedConditions)
                    .error(rewriter.rewrite(errorRule.getError()));
        }

        return rule.withConditions(transformedConditions);
    }

    private Condition transformCondition(Condition condition) {
        // Rewrite any references in the function
        TreeRewriter rewriter = createRewriter();
        LibraryFunction fn = condition.getFunction();
        LibraryFunction rewrittenFn = (LibraryFunction) rewriter.rewrite(fn);

        // If this condition assigns to a variable that should be renamed,
        // use the canonical name instead
        if (condition.getResult().isPresent()) {
            String varName = condition.getResult().get().toString();
            String canonicalName = variableRenameMap.get(varName);

            if (canonicalName != null) {
                // This variable is being consolidated, use the canonical name
                return Condition.builder()
                        .fn(rewrittenFn)
                        .result(Identifier.of(canonicalName))
                        .build();
            }
        }

        // No consolidation needed, but may still need reference rewriting
        if (rewrittenFn != fn) {
            return condition.toBuilder().fn(rewrittenFn).build();
        }

        return condition;
    }

    private TreeRewriter createRewriter() {
        if (variableRenameMap.isEmpty()) {
            return TreeRewriter.IDENTITY;
        }

        Map<String, Expression> replacements = new HashMap<>();
        for (Map.Entry<String, String> entry : variableRenameMap.entrySet()) {
            replacements.put(entry.getKey(), Expression.getReference(Identifier.of(entry.getValue())));
        }

        return TreeRewriter.forReplacements(replacements);
    }
}
