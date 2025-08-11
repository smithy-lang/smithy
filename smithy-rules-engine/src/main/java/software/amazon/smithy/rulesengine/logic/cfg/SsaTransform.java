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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
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
 * become "x", "x_1", "x_2", etc. Without this transform, the BDD compilation would confuse divergent paths that have
 * the same variable name.
 *
 * <p>Note that this transform is only applied when the reassignment is done using different
 * arguments than previously seen assignments of the same variable name.
 */
final class SsaTransform {

    // Stack of scopes, each mapping original variable names to their current SSA names
    private final Deque<Map<String, String>> scopeStack = new ArrayDeque<>();
    private final Map<Condition, Condition> rewrittenConditions = new IdentityHashMap<>();
    private final Map<Rule, Rule> rewrittenRules = new IdentityHashMap<>();
    private final VariableAnalysis variableAnalysis;
    private final ReferenceRewriter referenceRewriter;

    private SsaTransform(VariableAnalysis variableAnalysis) {
        // Start with an empty global scope
        scopeStack.push(new HashMap<>());
        this.variableAnalysis = variableAnalysis;

        // Create a reference rewriter that uses our scope resolution
        this.referenceRewriter = new ReferenceRewriter(
                ref -> {
                    String originalName = ref.getName().toString();
                    String uniqueName = resolveReference(originalName);
                    return Expression.getReference(Identifier.of(uniqueName));
                },
                this::needsRewriting);
    }

    static EndpointRuleSet transform(EndpointRuleSet ruleSet) {
        ruleSet = CoalesceTransform.transform(ruleSet);

        // Use VariableAnalysis to get all the information we need
        VariableAnalysis analysis = VariableAnalysis.analyze(ruleSet);

        // Rewrite with the pre-computed mappings
        SsaTransform ssaTransform = new SsaTransform(analysis);
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

    // Enters a new scope, inheriting all variable mappings from the parent scope
    private void enterScope() {
        scopeStack.push(new HashMap<>(scopeStack.peek()));
    }

    // Exits the current scope, reverting to the parent scope's variable mappings
    private void exitScope() {
        if (scopeStack.size() <= 1) {
            throw new IllegalStateException("Cannot exit global scope");
        }
        scopeStack.pop();
    }

    // Rewrites a condition's bindings and references to use SSA names
    private Condition rewriteCondition(Condition condition) {
        boolean hasBinding = condition.getResult().isPresent();

        // Check cache for non-binding conditions
        if (!hasBinding) {
            Condition cached = rewrittenConditions.get(condition);
            if (cached != null) {
                return cached;
            }
        }

        LibraryFunction fn = condition.getFunction();
        Set<String> rewritableRefs = filterOutInputParameters(fn.getReferences());

        // Determine if this binding needs an SSA name
        String uniqueBindingName = null;
        boolean needsUniqueBinding = false;
        if (hasBinding) {
            String varName = condition.getResult().get().toString();
            Map<String, String> expressionMap = variableAnalysis.getExpressionMappings().get(varName);
            if (expressionMap != null) {
                uniqueBindingName = expressionMap.get(fn.toString());
                needsUniqueBinding = uniqueBindingName != null && !uniqueBindingName.equals(varName);
            }
        }

        // Early return if no rewriting needed
        if (!needsRewriting(rewritableRefs) && !needsUniqueBinding) {
            if (!hasBinding) {
                rewrittenConditions.put(condition, condition);
            }
            return condition;
        }

        // Rewrite the expression
        LibraryFunction rewrittenExpr = (LibraryFunction) referenceRewriter.rewrite(fn);
        boolean exprChanged = rewrittenExpr != fn;

        // Build the rewritten condition
        Condition rewritten;
        if (hasBinding && uniqueBindingName != null) {
            // Update scope with the SSA name
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

        // Cache non-binding conditions
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

    // Check if any references in scope need to be rewritten to SSA names
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

    // Rewrites a rule's conditions to use SSA names
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
        Endpoint endpoint = rule.getEndpoint();

        // Rewrite endpoint components to use SSA names
        Expression rewrittenUrl = referenceRewriter.rewrite(endpoint.getUrl());
        Map<String, List<Expression>> rewrittenHeaders = rewriteHeaders(endpoint.getHeaders());
        Map<Identifier, Literal> rewrittenProperties = rewriteProperties(endpoint.getProperties());

        boolean endpointChanged = rewrittenUrl != endpoint.getUrl()
                || !rewrittenHeaders.equals(endpoint.getHeaders())
                || !rewrittenProperties.equals(endpoint.getProperties());

        if (conditionsChanged || endpointChanged) {
            return EndpointRule.builder()
                    .description(rule.getDocumentation().orElse(null))
                    .conditions(rewrittenConditions)
                    .endpoint(Endpoint.builder()
                            .url(rewrittenUrl)
                            .headers(rewrittenHeaders)
                            .properties(rewrittenProperties)
                            .build());
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

    private Map<String, List<Expression>> rewriteHeaders(Map<String, List<Expression>> headers) {
        Map<String, List<Expression>> rewritten = new LinkedHashMap<>();
        for (Map.Entry<String, List<Expression>> entry : headers.entrySet()) {
            List<Expression> rewrittenValues = new ArrayList<>();
            for (Expression expr : entry.getValue()) {
                rewrittenValues.add(referenceRewriter.rewrite(expr));
            }
            rewritten.put(entry.getKey(), rewrittenValues);
        }
        return rewritten;
    }

    private Map<Identifier, Literal> rewriteProperties(Map<Identifier, Literal> properties) {
        Map<Identifier, Literal> rewritten = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Literal> entry : properties.entrySet()) {
            Expression rewrittenExpr = referenceRewriter.rewrite(entry.getValue());
            if (!(rewrittenExpr instanceof Literal)) {
                throw new IllegalStateException("Property value must be a literal");
            }
            rewritten.put(entry.getKey(), (Literal) rewrittenExpr);
        }
        return rewritten;
    }

    private String resolveReference(String originalName) {
        // Input parameters are never rewritten
        return variableAnalysis.getInputParams().contains(originalName)
                ? originalName
                : scopeStack.peek().getOrDefault(originalName, originalName);
    }
}
