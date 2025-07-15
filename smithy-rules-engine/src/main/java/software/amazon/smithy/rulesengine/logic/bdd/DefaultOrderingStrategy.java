/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.logic.ConditionInfo;

/**
 * Orders conditions for BDD construction while respecting variable dependencies.
 *
 * <p>The ordering ensures that:
 * <ul>
 *   <li>Variables are defined before they are used</li>
 *   <li>isSet checks come before value checks for the same variable</li>
 *   <li>Simpler conditions are evaluated first (when dependencies allow)</li>
 * </ul>
 */
final class DefaultOrderingStrategy {

    private DefaultOrderingStrategy() {}

    static List<Condition> orderConditions(Condition[] conditions, Map<Condition, ConditionInfo> conditionInfos) {
        return sort(conditions, new ConditionDependencyGraph(conditionInfos), conditionInfos);
    }

    private static List<Condition> sort(
            Condition[] conditions,
            ConditionDependencyGraph deps,
            Map<Condition, ConditionInfo> infos
    ) {
        List<Condition> result = new ArrayList<>();
        Set<Condition> visited = new HashSet<>();
        Set<Condition> visiting = new HashSet<>();

        // Sort conditions by priority
        List<Condition> queue = new ArrayList<>();
        Collections.addAll(queue, conditions);

        queue.sort(Comparator
                // fewer deps first
                .comparingInt((Condition c) -> deps.getDependencies(c).size())
                // isSet() before everything else
                .thenComparingInt(c -> c.getFunction().getFunctionDefinition() == IsSet.getDefinition() ? 0 : 1)
                // variable-defining conditions first
                .thenComparingInt(c -> infos.get(c).getReturnVariable() != null ? 0 : 1)
                // fewer references first
                .thenComparingInt(c -> infos.get(c).getReferences().size())
                // stable tie-breaker
                .thenComparing(Condition::toString));

        // Visit in priority order
        for (Condition cond : queue) {
            if (!visited.contains(cond)) {
                visit(cond, deps, visited, visiting, result, infos);
            }
        }

        return result;
    }

    private static void visit(
            Condition cond,
            ConditionDependencyGraph depGraph,
            Set<Condition> visited,
            Set<Condition> visiting,
            List<Condition> result,
            Map<Condition, ConditionInfo> infos
    ) {
        if (visiting.contains(cond)) {
            throw new IllegalStateException("Circular dependency detected involving: " + cond);
        }

        if (visited.contains(cond)) {
            return;
        }

        visiting.add(cond);

        // Visit dependencies first
        Set<Condition> deps = depGraph.getDependencies(cond);
        if (!deps.isEmpty()) {
            List<Condition> sortedDeps = new ArrayList<>(deps);
            sortedDeps.sort(Comparator.comparingInt(c -> infos.get(c).getComplexity()));

            for (Condition dep : sortedDeps) {
                visit(dep, depGraph, visited, visiting, result, infos);
            }
        }

        visiting.remove(cond);
        visited.add(cond);
        result.add(cond);
    }
}
