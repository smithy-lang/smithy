/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;

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
    private final List<Condition> conditions;
    private final Map<Condition, Set<Condition>> dependencies;
    private final Map<String, Set<Condition>> variableDefiners;
    private final Map<String, Set<Condition>> isSetConditions;

    /**
     * Creates a dependency graph by analyzing the given conditions.
     *
     * @param conditions the conditions to analyze
     */
    public ConditionDependencyGraph(List<Condition> conditions) {
        this.conditions = Collections.unmodifiableList(new ArrayList<>(conditions));
        this.variableDefiners = new LinkedHashMap<>();
        this.isSetConditions = new LinkedHashMap<>();

        // Categorize all conditions
        for (Condition cond : conditions) {
            // Track variable definition
            if (cond.getResult().isPresent()) {
                String definedVar = cond.getResult().get().toString();
                variableDefiners.computeIfAbsent(definedVar, k -> new LinkedHashSet<>()).add(cond);
            }

            // Track isSet conditions
            if (isIsset(cond)) {
                for (String var : cond.getFunction().getReferences()) {
                    isSetConditions.computeIfAbsent(var, k -> new LinkedHashSet<>()).add(cond);
                }
            }
        }

        // Compute dependencies
        Map<Condition, Set<Condition>> deps = new LinkedHashMap<>();
        for (Condition cond : conditions) {
            Set<Condition> condDeps = new LinkedHashSet<>();

            for (String usedVar : cond.getFunction().getReferences()) {
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
        return conditions.size();
    }

    private static boolean isIsset(Condition cond) {
        return cond.getFunction().getFunctionDefinition() == IsSet.getDefinition();
    }
}
