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
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.evaluation.type.OptionalType;
import software.amazon.smithy.rulesengine.language.evaluation.type.RecordType;
import software.amazon.smithy.rulesengine.language.evaluation.type.Type;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Coalesce;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.LibraryFunction;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;

/**
 * Coalesces bind-then-use patterns in conditions, identifying conditions that bind a variable followed immediately by
 * a condition that uses that variable, and merges them using coalesce.
 */
final class CoalesceTransform {
    private static final Logger LOGGER = Logger.getLogger(CoalesceTransform.class.getName());

    private final Map<CoalesceKey, Condition> coalesceCache = new HashMap<>();
    private int coalesceCount = 0;
    private int cacheHits = 0;
    private int skippedNoZeroValue = 0;
    private int skippedMultipleUses = 0;
    private final Set<String> skippedRecordTypes = new HashSet<>();

    static EndpointRuleSet transform(EndpointRuleSet ruleSet) {
        CoalesceTransform transform = new CoalesceTransform();

        List<Rule> transformedRules = new ArrayList<>();
        for (Rule rule : ruleSet.getRules()) {
            transformedRules.add(transform.transformRule(rule));
        }

        if (LOGGER.isLoggable(Level.INFO)) {
            StringBuilder msg = new StringBuilder();
            msg.append("\n=== Coalescing Transform Complete ===\n");
            msg.append("Total: ").append(transform.coalesceCount).append(" coalesced, ");
            msg.append(transform.cacheHits).append(" cache hits, ");
            msg.append(transform.skippedNoZeroValue).append(" skipped (no zero value), ");
            msg.append(transform.skippedMultipleUses).append(" skipped (multiple uses)");
            if (!transform.skippedRecordTypes.isEmpty()) {
                msg.append("\nSkipped record-returning functions: ").append(transform.skippedRecordTypes);
            }
            LOGGER.info(msg.toString());
        }

        return EndpointRuleSet.builder()
                .parameters(ruleSet.getParameters())
                .rules(transformedRules)
                .version(ruleSet.getVersion())
                .build();
    }

    private Rule transformRule(Rule rule) {
        Set<Condition> eliminatedConditions = new HashSet<>();
        List<Condition> conditions = rule.getConditions();
        Map<String, Integer> localVarUsage = countLocalVariableUsage(conditions);
        List<Condition> transformedConditions = transformConditions(conditions, eliminatedConditions, localVarUsage);

        if (rule instanceof TreeRule) {
            TreeRule treeRule = (TreeRule) rule;
            List<Rule> transformedNestedRules = new ArrayList<>();
            boolean nestedChanged = false;

            for (Rule nestedRule : treeRule.getRules()) {
                Rule transformedNested = transformRule(nestedRule);
                transformedNestedRules.add(transformedNested);
                if (transformedNested != nestedRule) {
                    nestedChanged = true;
                }
            }

            if (!transformedConditions.equals(conditions) || nestedChanged) {
                return TreeRule.builder()
                        .description(rule.getDocumentation().orElse(null))
                        .conditions(transformedConditions)
                        .treeRule(transformedNestedRules);
            }
        } else if (!transformedConditions.equals(conditions)) {
            // For other rule types, just update conditions
            return rule.withConditions(transformedConditions);
        }

        return rule;
    }

    private Map<String, Integer> countLocalVariableUsage(List<Condition> conditions) {
        Map<String, Integer> usage = new HashMap<>();

        // Count how many times each variable is used within this specific rule
        for (Condition condition : conditions) {
            for (String ref : condition.getFunction().getReferences()) {
                usage.merge(ref, 1, Integer::sum);
            }
        }

        return usage;
    }

    private List<Condition> transformConditions(
            List<Condition> conditions,
            Set<Condition> eliminatedConditions,
            Map<String, Integer> localVarUsage
    ) {
        List<Condition> result = new ArrayList<>();

        for (int i = 0; i < conditions.size(); i++) {
            Condition current = conditions.get(i);
            if (eliminatedConditions.contains(current)) {
                continue;
            }

            // Check if this is a bind that can be coalesced with the next condition
            if (i + 1 < conditions.size() && current.getResult().isPresent()) {
                String var = current.getResult().get().toString();
                Condition next = conditions.get(i + 1);

                if (canCoalesce(var, current, next, localVarUsage)) {
                    // Create coalesced condition
                    Condition coalesced = createCoalescedCondition(current, next, var);
                    result.add(coalesced);
                    // Mark both conditions as eliminated
                    eliminatedConditions.add(current);
                    eliminatedConditions.add(next);
                    // Skip the next condition
                    i++;
                    continue;
                }
            }

            // No coalescing possible, keep the condition as-is
            result.add(current);
        }

        return result;
    }

    private boolean canCoalesce(String var, Condition bind, Condition use, Map<String, Integer> localVarUsage) {
        if (!use.getFunction().getReferences().contains(var)) {
            // The use condition must reference the variable
            return false;
        } else if (use.getFunction().getFunctionDefinition() == IsSet.getDefinition()) {
            // Never coalesce into presence checks (isSet)
            return false;
        }

        // Check if variable is only used once in this local rule context (even if it appears multiple times globally)
        Integer localUses = localVarUsage.get(var);
        if (localUses == null || localUses > 1) {
            skippedMultipleUses++;
            return false;
        }

        // Get the actual return type (could be Optional<T> or T)
        Type type = bind.getFunction().getFunctionDefinition().getReturnType();

        // Check if we can get a zero value for this type. For OptionalType, we use the inner type's zero value
        Type innerType = type;
        if (type instanceof OptionalType) {
            innerType = ((OptionalType) type).inner();
        }

        if (innerType instanceof RecordType) {
            skippedNoZeroValue++;
            skippedRecordTypes.add(bind.getFunction().getName());
            return false;
        }

        if (!innerType.getZeroValue().isPresent()) {
            skippedNoZeroValue++;
            return false;
        }

        return true;
    }

    private Condition createCoalescedCondition(Condition bind, Condition use, String var) {
        LibraryFunction bindExpr = bind.getFunction();
        LibraryFunction useExpr = use.getFunction();

        // Get the type and its zero value
        Type type = bindExpr.getFunctionDefinition().getReturnType();
        Type innerType = type;
        if (type instanceof OptionalType) {
            innerType = ((OptionalType) type).inner();
        }

        Literal zero = innerType.getZeroValue().get();

        // Create cache key based on canonical representations
        String bindCanonical = bindExpr.canonicalize().toString();
        String zeroCanonical = zero.toString();
        String useCanonical = useExpr.canonicalize().toString();
        String resultVar = use.getResult().map(Identifier::toString).orElse("");

        CoalesceKey key = new CoalesceKey(bindCanonical, zeroCanonical, useCanonical, var, resultVar);
        Condition cached = coalesceCache.get(key);
        if (cached != null) {
            cacheHits++;
            return cached;
        }

        Expression coalesced = Coalesce.ofExpressions(bindExpr, zero);

        // Replace the variable reference in the use expression
        Map<String, Expression> replacements = new HashMap<>();
        replacements.put(var, coalesced);
        ReferenceRewriter rewriter = ReferenceRewriter.forReplacements(replacements);
        Expression replaced = rewriter.rewrite(useExpr);
        LibraryFunction canonicalized = ((LibraryFunction) replaced).canonicalize();

        Condition.Builder builder = Condition.builder().fn(canonicalized);
        if (use.getResult().isPresent()) {
            builder.result(use.getResult().get());
        }

        Condition result = builder.build();
        coalesceCache.put(key, result);
        coalesceCount++;

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Coalesced #" + coalesceCount + ":\n"
                    + "  " + var + " = " + bind.getFunction() + "\n"
                    + "  " + use.getFunction() + "\n"
                    + "  => " + canonicalized);
        }

        return result;
    }

    private static final class CoalesceKey {
        final String bindFunction;
        final String zeroValue;
        final String useFunction;
        final String replacedVar;
        final String resultVar;
        final int hashCode;

        CoalesceKey(String bindFunction, String zeroValue, String useFunction, String replacedVar, String resultVar) {
            this.bindFunction = bindFunction;
            this.zeroValue = zeroValue;
            this.useFunction = useFunction;
            this.replacedVar = replacedVar;
            this.resultVar = resultVar;
            this.hashCode = Objects.hash(bindFunction, zeroValue, useFunction, replacedVar, resultVar);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            } else if (!(o instanceof CoalesceKey)) {
                return false;
            }
            CoalesceKey that = (CoalesceKey) o;
            return bindFunction.equals(that.bindFunction) && zeroValue.equals(that.zeroValue)
                    && useFunction.equals(that.useFunction)
                    && replacedVar.equals(that.replacedVar)
                    && resultVar.equals(that.resultVar);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }
    }
}
