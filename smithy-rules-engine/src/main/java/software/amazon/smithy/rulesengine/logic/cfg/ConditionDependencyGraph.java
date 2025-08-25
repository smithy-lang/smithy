/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;

/**
 * Graph of dependencies between conditions based on variable definitions and usage.
 *
 * <p>This class performs AST analysis once to extract:
 * <ul>
 *   <li>Variable definitions - which conditions define which variables</li>
 *   <li>Variable usage - which conditions use which variables</li>
 *   <li>Dependencies - which conditions must come before others</li>
 * </ul>
 */
public final class ConditionDependencyGraph {
    private final List<Condition> conditions;
    private final Map<Condition, Integer> conditionToIndex;
    private final Map<Condition, Set<Condition>> dependencies;
    private final Map<String, Set<Condition>> variableDefiners;
    private final Map<String, Set<Condition>> isSetConditions;

    // Indexed dependency information for fast access
    private final List<Set<Integer>> predecessors;
    private final List<Set<Integer>> successors;

    /**
     * Creates a dependency graph by analyzing the given conditions.
     *
     * @param conditions the conditions to analyze
     */
    public ConditionDependencyGraph(List<Condition> conditions) {
        this.conditions = Collections.unmodifiableList(new ArrayList<>(conditions));
        this.conditionToIndex = new HashMap<>();
        this.variableDefiners = new HashMap<>();
        this.isSetConditions = new HashMap<>();

        int n = conditions.size();
        for (int i = 0; i < n; i++) {
            conditionToIndex.put(conditions.get(i), i);
        }

        // Initialize indexed structures
        this.predecessors = new ArrayList<>(n);
        this.successors = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            predecessors.add(new HashSet<>());
            successors.add(new HashSet<>());
        }

        // Categorize all conditions
        for (Condition cond : conditions) {
            // Track variable definition
            if (cond.getResult().isPresent()) {
                String definedVar = cond.getResult().get().toString();
                variableDefiners.computeIfAbsent(definedVar, k -> new HashSet<>()).add(cond);
            }

            // Track isSet conditions
            if (isIsSet(cond)) {
                for (String var : cond.getFunction().getReferences()) {
                    isSetConditions.computeIfAbsent(var, k -> new HashSet<>()).add(cond);
                }
            }
        }

        // Compute dependencies
        Map<Condition, Set<Condition>> deps = new HashMap<>();
        Map<Identifier, Set<Integer>> producers = new HashMap<>();
        Map<Identifier, Set<Integer>> isSetters = new HashMap<>();

        // Build producer and isSet indices using Identifier
        for (int i = 0; i < n; i++) {
            Condition c = conditions.get(i);

            if (c.getResult().isPresent()) {
                Identifier var = c.getResult().get();
                producers.computeIfAbsent(var, k -> new HashSet<>()).add(i);
            }

            if (isIsSet(c)) {
                for (String ref : c.getFunction().getReferences()) {
                    Identifier var = Identifier.of(ref);
                    isSetters.computeIfAbsent(var, k -> new HashSet<>()).add(i);
                }
            }
        }

        // Build both object-based and index-based dependencies
        for (int i = 0; i < n; i++) {
            Condition cond = conditions.get(i);
            Set<Condition> condDeps = new HashSet<>();

            for (String usedVar : cond.getFunction().getReferences()) {
                // Object-based dependencies
                condDeps.addAll(variableDefiners.getOrDefault(usedVar, Collections.emptySet()));
                if (!isIsSet(cond)) {
                    condDeps.addAll(isSetConditions.getOrDefault(usedVar, Collections.emptySet()));
                }

                // Index-based dependencies
                Identifier var = Identifier.of(usedVar);
                for (int prod : producers.getOrDefault(var, Collections.emptySet())) {
                    if (prod != i) {
                        predecessors.get(i).add(prod);
                        successors.get(prod).add(i);
                    }
                }

                if (!isIsSet(cond)) {
                    for (int setter : isSetters.getOrDefault(var, Collections.emptySet())) {
                        if (setter != i) {
                            predecessors.get(i).add(setter);
                            successors.get(setter).add(i);
                        }
                    }
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
     * Gets the predecessors (dependencies) for a condition by index.
     *
     * @param index the condition index
     * @return set of predecessor indices
     */
    public Set<Integer> getPredecessors(int index) {
        return predecessors.get(index);
    }

    /**
     * Gets the successors (dependents) for a condition by index.
     *
     * @param index the condition index
     * @return set of successor indices
     */
    public Set<Integer> getSuccessors(int index) {
        return successors.get(index);
    }

    /**
     * Gets the number of predecessors for a condition.
     *
     * @param index the condition index
     * @return the predecessor count
     */
    public int getPredecessorCount(int index) {
        return predecessors.get(index).size();
    }

    /**
     * Gets the number of successors for a condition.
     *
     * @param index the condition index
     * @return the successor count
     */
    public int getSuccessorCount(int index) {
        return successors.get(index).size();
    }

    /**
     * Checks if there's a dependency from one condition to another.
     *
     * @param from the dependent condition index
     * @param to the dependency condition index
     * @return true if 'from' depends on 'to'
     */
    public boolean hasDependency(int from, int to) {
        return predecessors.get(from).contains(to);
    }

    /**
     * Creates order constraints for a specific ordering of conditions.
     *
     * @param ordering the ordering to compute constraints for
     * @return the order constraints
     */
    public OrderConstraints createOrderConstraints(List<Condition> ordering) {
        return new OrderConstraints(ordering);
    }

    /**
     * Gets the index mapping for conditions.
     *
     * @return map from condition to index
     */
    public Map<Condition, Integer> getConditionToIndex() {
        return Collections.unmodifiableMap(conditionToIndex);
    }

    /**
     * Gets the number of conditions in this dependency graph.
     *
     * @return the number of conditions
     */
    public int size() {
        return conditions.size();
    }

    private static boolean isIsSet(Condition cond) {
        return cond.getFunction().getFunctionDefinition() == IsSet.getDefinition();
    }

    /**
     * Order-specific constraints for a particular condition ordering.
     */
    public final class OrderConstraints {
        private final Condition[] orderedConditions;
        private final Map<Condition, Integer> orderIndex;
        private final int[] minValidPosition;
        private final int[] maxValidPosition;

        private OrderConstraints(List<Condition> ordering) {
            int n = ordering.size();
            if (n != conditions.size()) {
                throw new IllegalArgumentException(
                        "Ordering size (" + n + ") doesn't match dependency graph size (" + conditions.size() + ")");
            }

            this.orderedConditions = ordering.toArray(new Condition[0]);
            this.orderIndex = new HashMap<>(n * 2);
            this.minValidPosition = new int[n];
            this.maxValidPosition = new int[n];

            // Build index mapping for this ordering
            for (int i = 0; i < n; i++) {
                orderIndex.put(orderedConditions[i], i);
            }

            // Compute valid positions based on dependencies
            for (int i = 0; i < n; i++) {
                maxValidPosition[i] = n - 1; // Initialize max position

                Condition cond = orderedConditions[i];
                Integer originalIdx = conditionToIndex.get(cond);
                if (originalIdx == null) {
                    throw new IllegalArgumentException("Condition not in dependency graph: " + cond);
                }

                // Check all dependencies
                for (int depIdx : predecessors.get(originalIdx)) {
                    Condition depCond = conditions.get(depIdx);
                    Integer depOrderIdx = orderIndex.get(depCond);
                    if (depOrderIdx != null) {
                        // This condition must come after its dependency
                        minValidPosition[i] = Math.max(minValidPosition[i], depOrderIdx + 1);
                        // The dependency must come before this condition
                        maxValidPosition[depOrderIdx] = Math.min(maxValidPosition[depOrderIdx], i - 1);
                    }
                }
            }
        }

        /**
         * Checks if moving a condition from one position to another would violate dependencies.
         *
         * @param from current position
         * @param to target position
         * @return true if the move is valid
         */
        public boolean canMove(int from, int to) {
            return from == to || (to >= minValidPosition[from] && to <= maxValidPosition[from]);
        }

        /**
         * Gets the minimum valid position for a condition.
         *
         * @param positionIndex the position index in the ordering
         * @return the minimum position where this condition can be placed
         */
        public int getMinValidPosition(int positionIndex) {
            return minValidPosition[positionIndex];
        }

        /**
         * Gets the maximum valid position for a condition.
         *
         * @param positionIndex the position index in the ordering
         * @return the maximum position where this condition can be placed
         */
        public int getMaxValidPosition(int positionIndex) {
            return maxValidPosition[positionIndex];
        }
    }
}
