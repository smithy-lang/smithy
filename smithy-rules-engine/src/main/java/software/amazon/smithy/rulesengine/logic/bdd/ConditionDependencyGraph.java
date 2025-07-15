/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.logic.ConditionInfo;

/**
 * Immutable graph of dependencies between conditions.
 *
 * <p>This class performs the expensive AST analysis once to extract:
 * <ul>
 *   <li>Variable definitions - which conditions define which variables</li>
 *   <li>Variable usage - which conditions use which variables</li>
 *   <li>Raw dependencies - which conditions must come before others</li>
 * </ul>
 */
final class ConditionDependencyGraph {
    private final Map<Condition, ConditionInfo> conditionInfos;
    private final Map<Condition, Set<Condition>> dependencies;
    private final Map<String, Set<Condition>> variableDefiners;
    private final Map<String, Set<Condition>> isSetConditions;

    /**
     * Creates a dependency graph by analyzing the given conditions.
     *
     * @param conditionInfos metadata about each condition
     */
    public ConditionDependencyGraph(Map<Condition, ConditionInfo> conditionInfos) {
        this.conditionInfos = Collections.unmodifiableMap(new LinkedHashMap<>(conditionInfos));
        this.variableDefiners = new LinkedHashMap<>();
        this.isSetConditions = new LinkedHashMap<>();

        // Categorize all conditions
        for (Map.Entry<Condition, ConditionInfo> entry : conditionInfos.entrySet()) {
            Condition cond = entry.getKey();
            ConditionInfo info = entry.getValue();

            // Track variable definition
            String definedVar = info.getReturnVariable();
            if (definedVar != null) {
                variableDefiners.computeIfAbsent(definedVar, k -> new LinkedHashSet<>()).add(cond);
            }

            // Track isSet conditions
            if (isIsset(cond)) {
                for (String var : info.getReferences()) {
                    isSetConditions.computeIfAbsent(var, k -> new LinkedHashSet<>()).add(cond);
                }
            }
        }

        // Compute dependencies
        Map<Condition, Set<Condition>> deps = new LinkedHashMap<>();
        for (Map.Entry<Condition, ConditionInfo> entry : conditionInfos.entrySet()) {
            Condition cond = entry.getKey();
            ConditionInfo info = entry.getValue();

            Set<Condition> condDeps = new LinkedHashSet<>();

            for (String usedVar : info.getReferences()) {
                // Must come after any condition that defines this variable
                condDeps.addAll(variableDefiners.getOrDefault(usedVar, Collections.emptySet()));

                // Non-isSet conditions must come after isSet checks for undefined variables
                if (!isIsset(cond)) {
                    condDeps.addAll(isSetConditions.getOrDefault(usedVar, Collections.emptySet()));
                }
            }

            condDeps.remove(cond); // Remove self-dependencies
            if (!condDeps.isEmpty()) {
                deps.put(cond, Collections.unmodifiableSet(condDeps));
            }
        }

        this.dependencies = Collections.unmodifiableMap(deps);
    }

    /**
     * Gets the dependencies for a condition.
     *
     * @param condition the condition to query
     * @return set of conditions that must come before it (never null)
     */
    public Set<Condition> getDependencies(Condition condition) {
        return dependencies.getOrDefault(condition, Collections.emptySet());
    }

    /**
     * Gets the number of conditions in this dependency graph.
     *
     * @return the number of conditions
     */
    public int size() {
        return conditionInfos.size();
    }

    private static boolean isIsset(Condition cond) {
        return cond.getFunction().getFunctionDefinition() == IsSet.getDefinition();
    }
}
