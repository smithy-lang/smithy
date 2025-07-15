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
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.Endpoint;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Template;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.FunctionNode;
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
 * Transforms a decision tree into Static Single Assignment (SSA) form.
 *
 * <p>This transformation ensures that each variable is assigned exactly once by renaming variables when they are
 * reassigned in different parts of the tree. For example, if variable "x" is assigned in multiple branches, they
 * become "x", "x_1", "x_2", etc. Without this transform, the BDD compilation would confuse divergent paths that have
 * the same variable name.
 *
 * <p>Note that this transform is only applied when the reassignment is done using different
 * arguments than previously seen assignments of the same variable name.
 *
 * <p>TODO: This transform does not yet introduce phi nodes at control flow merge points.
 *          We need to add an OR function to the rules engine to do that.
 */
final class SsaTransform {

    // Stack of scopes, each mapping original variable names to their current SSA names
    private final Deque<Map<String, String>> scopeStack = new ArrayDeque<>();

    // Cache of already rewritten conditions to avoid redundant work
    private final Map<Condition, Condition> rewrittenConditions = new IdentityHashMap<>();

    // Cache of already rewritten rules
    private final Map<Rule, Rule> rewrittenRules = new IdentityHashMap<>();

    // Set of input parameter names that should never be rewritten
    private final Set<String> inputParams;

    // Map from variable name -> expression -> SSA name
    // Pre-computed to ensure consistent naming across the tree
    private final Map<String, Map<String, String>> variableExpressionMappings;

    private SsaTransform(Set<String> inputParams, Map<String, Map<String, String>> variableExpressionMappings) {
        // Start with an empty global scope
        scopeStack.push(new HashMap<>());
        this.inputParams = inputParams;
        this.variableExpressionMappings = variableExpressionMappings;
    }

    static EndpointRuleSet transform(EndpointRuleSet ruleSet) {
        Set<String> inputParameters = extractInputParameters(ruleSet);

        // Collect all variable bindings and create unique names for each unique expression
        Map<String, Set<String>> variableBindings = collectVariableBindings(ruleSet.getRules());
        Map<String, Map<String, String>> variableExpressionMappings = createExpressionMappings(variableBindings);

        // Rewrite with the pre-computed mappings
        SsaTransform ssaTransform = new SsaTransform(inputParameters, variableExpressionMappings);
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

    // Collect a set of input parameter names. We use these to know that expressions that only work with an input
    // parameter and use the same arguments can be kept as-is rather than need a cloned and renamed assignment.
    private static Set<String> extractInputParameters(EndpointRuleSet ruleSet) {
        Set<String> inputParameters = new HashSet<>();
        for (Parameter param : ruleSet.getParameters()) {
            inputParameters.add(param.getName().toString());
        }
        return inputParameters;
    }

    private static Map<String, Set<String>> collectVariableBindings(List<Rule> rules) {
        Map<String, Set<String>> variableBindings = new HashMap<>();
        collectBindingsFromRules(rules, variableBindings);
        return variableBindings;
    }

    private static void collectBindingsFromRules(List<Rule> rules, Map<String, Set<String>> variableBindings) {
        for (Rule rule : rules) {
            collectBindingsFromRule(rule, variableBindings);
        }
    }

    private static void collectBindingsFromRule(Rule rule, Map<String, Set<String>> variableBindings) {
        for (Condition condition : rule.getConditions()) {
            if (condition.getResult().isPresent()) {
                String varName = condition.getResult().get().toString();
                String expression = condition.getFunction().toString();
                variableBindings.computeIfAbsent(varName, k -> new HashSet<>()).add(expression);
            }
        }

        if (rule instanceof TreeRule) {
            TreeRule treeRule = (TreeRule) rule;
            collectBindingsFromRules(treeRule.getRules(), variableBindings);
        }
    }

    /**
     * Creates a mapping from variable name -> expression -> SSA name.
     * Variables assigned multiple times get unique SSA names (x, x_1, x_2, etc).
     */
    private static Map<String, Map<String, String>> createExpressionMappings(Map<String, Set<String>> bindings) {
        Map<String, Map<String, String>> result = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : bindings.entrySet()) {
            String varName = entry.getKey();
            Set<String> expressions = entry.getValue();
            result.put(varName, createMappingForVariable(varName, expressions));
        }

        return result;
    }

    private static Map<String, String> createMappingForVariable(String varName, Set<String> expressions) {
        Map<String, String> mapping = new HashMap<>();

        if (expressions.size() == 1) {
            // Only one expression for this variable, so no SSA renaming needed
            String expression = expressions.iterator().next();
            mapping.put(expression, varName);
        } else {
            // Multiple expressions, so create unique SSA names
            List<String> sortedExpressions = new ArrayList<>(expressions);
            sortedExpressions.sort(String::compareTo); // Ensure deterministic ordering

            for (int i = 0; i < sortedExpressions.size(); i++) {
                String expression = sortedExpressions.get(i);
                String uniqueName = (i == 0) ? varName : varName + "_" + i;
                mapping.put(expression, uniqueName);
            }
        }

        return mapping;
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
            Map<String, String> expressionMap = variableExpressionMappings.get(varName);
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
        LibraryFunction rewrittenExpr = (LibraryFunction) rewriteExpression(fn);
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
        if (references.isEmpty() || inputParams.isEmpty()) {
            return references;
        }
        Set<String> filtered = new HashSet<>(references);
        filtered.removeAll(inputParams);
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
        Expression rewrittenUrl = rewriteExpression(endpoint.getUrl());
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
        Expression rewrittenError = rewriteExpression(rule.getError());

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
                rewrittenValues.add(rewriteExpression(expr));
            }
            rewritten.put(entry.getKey(), rewrittenValues);
        }
        return rewritten;
    }

    private Map<Identifier, Literal> rewriteProperties(Map<Identifier, Literal> properties) {
        Map<Identifier, Literal> rewritten = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Literal> entry : properties.entrySet()) {
            Expression rewrittenExpr = rewriteExpression(entry.getValue());
            if (!(rewrittenExpr instanceof Literal)) {
                throw new IllegalStateException("Property value must be a literal");
            }
            rewritten.put(entry.getKey(), (Literal) rewrittenExpr);
        }
        return rewritten;
    }

    // Recursively rewrites an expression to use SSA names
    private Expression rewriteExpression(Expression expression) {
        if (!needsRewriting(expression)) {
            return expression;
        }

        if (expression instanceof StringLiteral) {
            return rewriteStringLiteral((StringLiteral) expression);
        } else if (expression instanceof TupleLiteral) {
            return rewriteTupleLiteral((TupleLiteral) expression);
        } else if (expression instanceof RecordLiteral) {
            return rewriteRecordLiteral((RecordLiteral) expression);
        } else if (expression instanceof Reference) {
            return rewriteReference((Reference) expression);
        } else if (expression instanceof LibraryFunction) {
            return rewriteLibraryFunction((LibraryFunction) expression);
        }

        return expression;
    }

    private Expression rewriteStringLiteral(StringLiteral str) {
        Template template = str.value();
        if (template.isStatic()) {
            return str;
        }

        StringBuilder templateBuilder = new StringBuilder();
        for (Template.Part part : template.getParts()) {
            if (part instanceof Template.Dynamic) {
                Template.Dynamic dynamic = (Template.Dynamic) part;
                Expression rewritten = rewriteExpression(dynamic.toExpression());
                templateBuilder.append('{').append(rewritten).append('}');
            } else {
                templateBuilder.append(((Template.Literal) part).getValue());
            }
        }
        return Literal.stringLiteral(Template.fromString(templateBuilder.toString()));
    }

    private Expression rewriteTupleLiteral(TupleLiteral tuple) {
        List<Literal> rewrittenMembers = new ArrayList<>();
        for (Literal member : tuple.members()) {
            rewrittenMembers.add((Literal) rewriteExpression(member));
        }
        return Literal.tupleLiteral(rewrittenMembers);
    }

    private Expression rewriteRecordLiteral(RecordLiteral record) {
        Map<Identifier, Literal> rewrittenMembers = new LinkedHashMap<>();
        for (Map.Entry<Identifier, Literal> entry : record.members().entrySet()) {
            rewrittenMembers.put(entry.getKey(), (Literal) rewriteExpression(entry.getValue()));
        }
        return Literal.recordLiteral(rewrittenMembers);
    }

    private Expression rewriteReference(Reference ref) {
        String originalName = ref.getName().toString();
        String uniqueName = resolveReference(originalName);
        return Expression.getReference(Identifier.of(uniqueName));
    }

    private Expression rewriteLibraryFunction(LibraryFunction fn) {
        List<Expression> rewrittenArgs = new ArrayList<>(fn.getArguments());
        rewrittenArgs.replaceAll(this::rewriteExpression);
        FunctionNode node = FunctionNode.builder()
                .name(Node.from(fn.getName()))
                .arguments(rewrittenArgs)
                .build();
        return fn.getFunctionDefinition().createFunction(node);
    }

    private String resolveReference(String originalName) {
        // Input parameters are never rewritten
        return inputParams.contains(originalName)
                ? originalName
                : scopeStack.peek().getOrDefault(originalName, originalName);
    }
}
