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
 * and other metadata needed for SSA transformation and optimization.
 */
final class VariableAnalysis {
    private final Set<String> inputParams;
    private final Map<String, Set<String>> bindings;
    private final Map<String, Integer> referenceCounts;
    private final Map<String, Map<String, String>> expressionMappings;
    private final Map<String, List<String>> expressionToVars;

    private VariableAnalysis(
            Set<String> inputParams,
            Map<String, Set<String>> bindings,
            Map<String, Integer> referenceCounts,
            Map<String, List<String>> expressionToVars
    ) {
        this.inputParams = inputParams;
        this.bindings = bindings;
        this.referenceCounts = referenceCounts;
        this.expressionToVars = expressionToVars;
        this.expressionMappings = createExpressionMappings(bindings);
    }

    static VariableAnalysis analyze(EndpointRuleSet ruleSet) {
        Set<String> inputParameters = extractInputParameters(ruleSet);

        AnalysisVisitor visitor = new AnalysisVisitor(inputParameters);
        for (Rule rule : ruleSet.getRules()) {
            visitor.visitRule(rule);
        }

        return new VariableAnalysis(
                inputParameters,
                visitor.bindings,
                visitor.referenceCounts,
                visitor.expressionToVars);
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
        Set<String> expressions = bindings.get(variableName);
        return expressions != null && expressions.size() == 1;
    }

    boolean hasMultipleBindings(String variableName) {
        Set<String> expressions = bindings.get(variableName);
        return expressions != null && expressions.size() > 1;
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
            Map<String, Set<String>> bindings
    ) {
        Map<String, Map<String, String>> result = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : bindings.entrySet()) {
            String varName = entry.getKey();
            Set<String> expressions = entry.getValue();
            result.put(varName, createMappingForVariable(varName, expressions));
        }
        return result;
    }

    private static Map<String, String> createMappingForVariable(
            String varName,
            Set<String> expressions
    ) {
        Map<String, String> mapping = new HashMap<>();

        if (expressions.size() == 1) {
            String expression = expressions.iterator().next();
            mapping.put(expression, varName);
        } else {
            List<String> sortedExpressions = new ArrayList<>(expressions);
            sortedExpressions.sort(String::compareTo);

            for (int i = 0; i < sortedExpressions.size(); i++) {
                String expression = sortedExpressions.get(i);
                String uniqueName = (i == 0) ? varName : varName + "_" + i;
                mapping.put(expression, uniqueName);
            }
        }

        return mapping;
    }

    private static class AnalysisVisitor {
        final Map<String, Set<String>> bindings = new HashMap<>();
        final Map<String, Integer> referenceCounts = new HashMap<>();
        final Map<String, List<String>> expressionToVars = new HashMap<>();

        private final Set<String> inputParams;

        AnalysisVisitor(Set<String> inputParams) {
            this.inputParams = inputParams;
        }

        void visitRule(Rule rule) {
            for (Condition condition : rule.getConditions()) {
                if (condition.getResult().isPresent()) {
                    String varName = condition.getResult().get().toString();
                    LibraryFunction fn = condition.getFunction();
                    String expression = fn.toString();
                    String canonical = fn.canonicalize().toString();

                    bindings.computeIfAbsent(varName, k -> new HashSet<>())
                            .add(expression);

                    expressionToVars.computeIfAbsent(canonical, k -> new ArrayList<>())
                            .add(varName);
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
                // Count all references, including input parameters
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
