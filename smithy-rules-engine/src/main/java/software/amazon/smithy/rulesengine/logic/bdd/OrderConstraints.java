/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;

/**
 * Order-specific constraints derived from a dependency graph.
 *
 * <p>This class efficiently computes position-based constraints for a specific ordering of conditions, using the
 * pre-computed dependency graph. It can be created cheaply for each new ordering during optimization.
 */
final class OrderConstraints {
    private final Condition[] conditions;
    private final Map<Condition, Integer> conditionToIndex;
    private final int[] minValidPosition;
    private final int[] maxValidPosition;

    /**
     * Creates order constraints for a specific ordering.
     *
     * @param graph the pre-computed dependency graph
     * @param conditions the conditions in their specific order
     */
    public OrderConstraints(ConditionDependencyGraph graph, List<Condition> conditions) {
        int n = conditions.size();
        if (n != graph.size()) {
            throw new IllegalArgumentException(
                    "Condition count (" + n + ") doesn't match dependency graph size (" + graph.size() + ")");
        }

        this.conditions = conditions.toArray(new Condition[0]);
        this.conditionToIndex = new HashMap<>(n * 2);
        this.minValidPosition = new int[n];
        this.maxValidPosition = new int[n];

        // Build index mapping
        for (int i = 0; i < n; i++) {
            conditionToIndex.put(this.conditions[i], i);
        }

        // Build dependencies and compute valid positions in one pass
        for (int i = 0; i < n; i++) {
            maxValidPosition[i] = n - 1; // Initialize max position
            for (Condition dep : graph.getDependencies(this.conditions[i])) {
                Integer depIndex = conditionToIndex.get(dep);
                if (depIndex != null) {
                    // This condition must come after its dependency
                    minValidPosition[i] = Math.max(minValidPosition[i], depIndex + 1);
                    // The dependency must come before this condition
                    maxValidPosition[depIndex] = Math.min(maxValidPosition[depIndex], i - 1);
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
     * @param conditionIndex the condition index
     * @return the minimum position where this condition can be placed
     */
    public int getMinValidPosition(int conditionIndex) {
        return minValidPosition[conditionIndex];
    }

    /**
     * Gets the maximum valid position for a condition.
     *
     * @param conditionIndex the condition index
     * @return the maximum position where this condition can be placed
     */
    public int getMaxValidPosition(int conditionIndex) {
        return maxValidPosition[conditionIndex];
    }
}
