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

    private VariableAnalysis(
            Set<String> inputParams,
            Map<String, Integer> bindingCounts,
            Map<String, Integer> referenceCounts,
            Map<String, Map<String, String>> expressionMappings
    ) {
        this.inputParams = inputParams;
        this.bindingCounts = bindingCounts;
        this.referenceCounts = referenceCounts;
        this.expressionMappings = expressionMappings;
    }

    static VariableAnalysis analyze(EndpointRuleSet ruleSet) {
        AnalysisVisitor visitor = new AnalysisVisitor();
        for (Rule rule : ruleSet.getRules()) {
            visitor.visitRule(rule);
        }

        return new VariableAnalysis(
                extractInputParameters(ruleSet),
                visitor.bindingCounts,
                visitor.referenceCounts,
                createExpressionMappings(visitor.bindings, visitor.bindingCounts));
    }

    Set<String> getInputParams() {
        return inputParams;
    }

    Map<String, Map<String, String>> getExpressionMappings() {
        return expressionMappings;
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
        // Check if variable is bound more than once, regardless of whether expressions are the same.
        // This is important because SSA may rewrite references in the expressions, making originally
        // identical expressions different after SSA. For example:
        //   Branch A: outpostId = getAttr(parsed, ...)
        //   Branch B: outpostId = getAttr(parsed, ...)
        // If "parsed" is renamed to "parsed_ssa_1" in branch A and "parsed_ssa_2" in branch B,
        // the expressions become different, but we've already decided not to rename "outpostId".
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
            Map<String, Integer> bindingCounts
    ) {
        Map<String, Map<String, String>> result = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : bindings.entrySet()) {
            String varName = entry.getKey();
            Set<String> expressions = entry.getValue();
            int bindingCount = bindingCounts.getOrDefault(varName, 0);
            result.put(varName, createMappingForVariable(varName, expressions, bindingCount));
        }
        return result;
    }

    private static Map<String, String> createMappingForVariable(
            String varName,
            Set<String> expressions,
            int bindingCount
    ) {
        Map<String, String> mapping = new HashMap<>();

        if (bindingCount <= 1 || expressions.size() == 1) {
            // Single binding or multiple bindings with the same expression: no SSA rename needed.
            // When multiple bindings have the same expression text, references inside may get
            // SSA-renamed differently in each scope, but that's fine: the resulting expressions
            // will differ and be treated as distinct BDD conditions. The binding name being the
            // same doesn't cause collisions since conditions are identified by their full content.
            String expression = expressions.iterator().next();
            mapping.put(expression, varName);
        } else {
            // Multiple bindings with different expressions: use SSA naming convention
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

        void visitRule(Rule rule) {
            for (Condition condition : rule.getConditions()) {
                if (condition.getResult().isPresent()) {
                    String varName = condition.getResult().get().toString();
                    LibraryFunction fn = condition.getFunction();
                    String expression = fn.toString();

                    bindings.computeIfAbsent(varName, k -> new HashSet<>()).add(expression);
                    // Track number of times variable is bound (not just unique expressions)
                    bindingCounts.merge(varName, 1, Integer::sum);
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
