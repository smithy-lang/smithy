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
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Template;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.RecordLiteral;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.StringLiteral;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.TupleLiteral;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;

/**
 * Analyzes variables in an endpoint rule set, collecting bindings, reference counts,
 * and expression mappings needed for SSA transformation.
 */
final class VariableAnalysis {
    private final Set<String> inputParams;
    private final Map<String, Integer> bindingCounts;
    private final Map<String, Integer> referenceCounts;
    private final Map<String, Map<String, String>> expressionMappings;
    private final Set<String> variablesNeedingSsa;

    private VariableAnalysis(
            Set<String> inputParams,
            Map<String, Integer> bindingCounts,
            Map<String, Integer> referenceCounts,
            Map<String, Map<String, String>> expressionMappings,
            Set<String> variablesNeedingSsa
    ) {
        this.inputParams = inputParams;
        this.bindingCounts = bindingCounts;
        this.referenceCounts = referenceCounts;
        this.expressionMappings = expressionMappings;
        this.variablesNeedingSsa = variablesNeedingSsa;
    }

    static VariableAnalysis analyze(EndpointRuleSet ruleSet) {
        AnalysisVisitor visitor = new AnalysisVisitor();
        for (Rule rule : ruleSet.getRules()) {
            visitor.visitRule(rule);
        }

        Set<String> needsSsa = computeVariablesNeedingSsa(
                visitor.bindings,
                visitor.bindingCounts,
                visitor.bindingReferences);

        return new VariableAnalysis(
                extractInputParameters(ruleSet),
                visitor.bindingCounts,
                visitor.referenceCounts,
                createExpressionMappings(visitor.bindings, visitor.bindingCounts, needsSsa),
                needsSsa);
    }

    Set<String> getInputParams() {
        return inputParams;
    }

    Map<String, Map<String, String>> getExpressionMappings() {
        return expressionMappings;
    }

    /**
     * Returns whether the variable needs SSA renaming due to multiple bindings with
     * different expressions or transitive dependencies on other SSA-renamed variables.
     */
    boolean needsSsaRenaming(String variableName) {
        return variablesNeedingSsa.contains(variableName);
    }

    int getReferenceCount(String variableName) {
        return referenceCounts.getOrDefault(variableName, 0);
    }

    boolean isReferencedOnce(String variableName) {
        return getReferenceCount(variableName) == 1;
    }

    boolean hasSingleBinding(String variableName) {
        Integer count = bindingCounts.get(variableName);
        return count != null && count == 1;
    }

    boolean hasMultipleBindings(String variableName) {
        Integer count = bindingCounts.get(variableName);
        return count != null && count > 1;
    }

    boolean isSafeToInline(String variableName) {
        return hasSingleBinding(variableName) && isReferencedOnce(variableName);
    }

    private static Set<String> extractInputParameters(EndpointRuleSet ruleSet) {
        Set<String> inputParameters = new HashSet<>();
        for (Parameter param : ruleSet.getParameters()) {
            inputParameters.add(param.getName().toString());
        }
        return inputParameters;
    }

    private static Map<String, Map<String, String>> createExpressionMappings(
            Map<String, Set<String>> bindings,
            Map<String, Integer> bindingCounts,
            Set<String> needsSsa
    ) {
        Map<String, Map<String, String>> result = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : bindings.entrySet()) {
            String varName = entry.getKey();
            Set<String> expressions = entry.getValue();
            int bindingCount = bindingCounts.getOrDefault(varName, 0);
            result.put(varName,
                    createMappingForVariable(varName,
                            expressions,
                            bindingCount,
                            needsSsa.contains(varName)));
        }
        return result;
    }

    /**
     * Computes which variables need SSA renaming using fixed-point iteration.
     *
     * <p>A variable needs SSA if:
     * <ul>
     *   <li>It has multiple bindings with different expression text, OR</li>
     *   <li>It has multiple bindings and references a variable that needs SSA (transitive)</li>
     * </ul>
     *
     * <p>The transitive case handles situations like:
     * <pre>
     *   Branch A: parts = split(input, delim, 0); part1 = coalesce(getAttr(parts, "[0]"), "")
     *   Branch B: parts = split(input, delim, 1); part1 = coalesce(getAttr(parts, "[0]"), "")
     * </pre>
     * Here {@code part1} has identical expression text in both branches, but {@code parts} will be
     * SSA-renamed, so {@code part1} must also be SSA-renamed to avoid shadowing in the flattened BDD.
     */
    private static Set<String> computeVariablesNeedingSsa(
            Map<String, Set<String>> bindings,
            Map<String, Integer> bindingCounts,
            Map<String, Set<String>> bindingReferences
    ) {
        Set<String> needsSsa = new HashSet<>();

        // Initial pass: variables with multiple bindings and different expressions need SSA.
        for (Map.Entry<String, Set<String>> entry : bindings.entrySet()) {
            String varName = entry.getKey();
            int bindingCount = bindingCounts.getOrDefault(varName, 0);
            if (bindingCount > 1 && entry.getValue().size() > 1) {
                needsSsa.add(varName);
            }
        }

        // Fixed-point: propagate SSA requirement through reference chains.
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Map.Entry<String, Set<String>> entry : bindings.entrySet()) {
                String varName = entry.getKey();
                if (needsSsa.contains(varName)) {
                    continue;
                }
                int bindingCount = bindingCounts.getOrDefault(varName, 0);
                if (bindingCount <= 1) {
                    continue;
                }
                // If any referenced variable needs SSA, this one does too.
                Set<String> refs = bindingReferences.get(varName);
                if (refs != null) {
                    for (String ref : refs) {
                        if (needsSsa.contains(ref)) {
                            needsSsa.add(varName);
                            changed = true;
                            break;
                        }
                    }
                }
            }
        }

        return needsSsa;
    }

    private static Map<String, String> createMappingForVariable(
            String varName,
            Set<String> expressions,
            int bindingCount,
            boolean needsSsa
    ) {
        Map<String, String> mapping = new HashMap<>();

        if (bindingCount <= 1 || !needsSsa) {
            // Single binding, or multiple bindings that don't need SSA renaming.
            String expression = expressions.iterator().next();
            mapping.put(expression, varName);
        } else {
            // Multiple bindings that need SSA: assign unique names.
            List<String> sortedExpressions = new ArrayList<>(expressions);
            sortedExpressions.sort(String::compareTo);
            for (int i = 0; i < sortedExpressions.size(); i++) {
                String expression = sortedExpressions.get(i);
                String uniqueName = varName + "_ssa_" + (i + 1);
                mapping.put(expression, uniqueName);
            }
        }

        return mapping;
    }

    private static class AnalysisVisitor {
        final Map<String, Set<String>> bindings = new HashMap<>();
        final Map<String, Integer> bindingCounts = new HashMap<>();
        final Map<String, Integer> referenceCounts = new HashMap<>();
        // Maps variable name -> set of variables referenced in its binding expressions
        final Map<String, Set<String>> bindingReferences = new HashMap<>();

        void visitRule(Rule rule) {
            for (Condition condition : rule.getConditions()) {
                if (condition.getResult().isPresent()) {
                    String varName = condition.getResult().get().toString();
                    LibraryFunction fn = condition.getFunction();
                    String expression = fn.toString();

                    bindings.computeIfAbsent(varName, k -> new HashSet<>()).add(expression);
                    // Track number of times variable is bound (not just unique expressions)
                    bindingCounts.merge(varName, 1, Integer::sum);
                    // Track which variables this binding references (for transitive SSA detection)
                    bindingReferences.computeIfAbsent(varName, k -> new HashSet<>()).addAll(fn.getReferences());
                }

                countReferences(condition.getFunction());
            }

            if (rule instanceof TreeRule) {
                TreeRule treeRule = (TreeRule) rule;
                for (Rule nestedRule : treeRule.getRules()) {
                    visitRule(nestedRule);
                }
            } else if (rule instanceof EndpointRule) {
                EndpointRule endpointRule = (EndpointRule) rule;
                Endpoint endpoint = endpointRule.getEndpoint();
                countReferences(endpoint.getUrl());
                endpoint.getHeaders()
                        .values()
                        .stream()
                        .flatMap(List::stream)
                        .forEach(this::countReferences);
                endpoint.getProperties()
                        .values()
                        .forEach(this::countReferences);
            } else if (rule instanceof ErrorRule) {
                countReferences(((ErrorRule) rule).getError());
            }
        }

        private void countReferences(Expression expression) {
            if (expression instanceof Reference) {
                Reference ref = (Reference) expression;
                String name = ref.getName().toString();
                referenceCounts.merge(name, 1, Integer::sum);
            } else if (expression instanceof StringLiteral) {
                StringLiteral str = (StringLiteral) expression;
                Template template = str.value();
                if (!template.isStatic()) {
                    for (Template.Part part : template.getParts()) {
                        if (part instanceof Template.Dynamic) {
                            Template.Dynamic dynamic = (Template.Dynamic) part;
                            countReferences(dynamic.toExpression());
                        }
                    }
                }
            } else if (expression instanceof LibraryFunction) {
                LibraryFunction fn = (LibraryFunction) expression;
                for (Expression arg : fn.getArguments()) {
                    countReferences(arg);
                }
            } else if (expression instanceof TupleLiteral) {
                TupleLiteral tuple = (TupleLiteral) expression;
                for (Literal member : tuple.members()) {
                    countReferences(member);
                }
            } else if (expression instanceof RecordLiteral) {
                RecordLiteral record = (RecordLiteral) expression;
                for (Literal value : record.members().values()) {
                    countReferences(value);
                }
            }
        }
    }
}
