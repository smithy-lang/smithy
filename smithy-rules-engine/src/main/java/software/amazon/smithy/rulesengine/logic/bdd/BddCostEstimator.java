/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.logic.ConditionCostModel;

/**
 * Estimates the average-case runtime cost of a BDD using probabilistic graph analysis.
 *
 * <p>This class calculates the expected total cost of evaluating a decision graph by summing the cost of
 * each condition weighted by the probability that it will actually be executed:
 * <pre>
 * E[C] = Σ (Cost(c) × P(Condition c is evaluated))
 * </pre>
 *
 * <p>This approach provides a high-fidelity signal for optimization loops, allowing the BDD builder to distinguish
 * between "expensive checks on hot paths" (critical to optimize) and "expensive checks on rare error paths"
 * (safe to ignore).
 */
final class BddCostEstimator {

    private final Map<Condition, Integer> costs;
    private final ToDoubleFunction<Condition> trueProbability;

    BddCostEstimator(
            List<Condition> conditions,
            ConditionCostModel costModel,
            ToDoubleFunction<Condition> trueProbability
    ) {
        this.costs = new IdentityHashMap<>();
        this.trueProbability = trueProbability;

        for (Condition c : conditions) {
            this.costs.put(c, costModel.cost(c));
        }
    }

    /**
     * Computes the expected evaluation cost for a BDD with the given ordering.
     *
     * @param bdd the BDD to analyze
     * @param ordering the condition ordering used to compile the BDD
     * @return the expected evaluation cost
     */
    double expectedCost(Bdd bdd, List<Condition> ordering) {
        int rootRef = bdd.getRootRef();
        if (!Bdd.isNodeReference(rootRef)) {
            return 0.0; // Terminal root, no conditions evaluated
        }

        // Build cost and probability arrays for the current ordering
        int size = ordering.size();
        int[] costArray = new int[size];
        double[] probArray = new double[size];

        for (int i = 0; i < size; i++) {
            Condition c = ordering.get(i);
            costArray[i] = costs.getOrDefault(c, 1);
            probArray[i] = trueProbability != null ? trueProbability.applyAsDouble(c) : 0.5;
        }

        // Compute expected evaluations per condition using single-pass topological traversal
        double[] expectedEvaluations = computeExpectedEvaluations(bdd, probArray);

        // Compute expected cost
        double totalCost = 0.0;
        for (int v = 0; v < expectedEvaluations.length && v < costArray.length; v++) {
            totalCost += expectedEvaluations[v] * costArray[v];
        }

        return totalCost;
    }

    /**
     * Computes expected evaluations for each condition position.
     *
     * <p>In a reduced BDD (ROBDD), each condition appears at most once per path,
     * so expected evaluations equals the probability of reaching any node testing that condition.
     *
     * @param bdd the BDD to analyze
     * @param ordering the condition ordering used to compile the BDD
     * @return array of expected evaluations indexed by position in ordering
     */
    double[] reachProbabilities(Bdd bdd, List<Condition> ordering) {
        int rootRef = bdd.getRootRef();
        if (!Bdd.isNodeReference(rootRef)) {
            return new double[bdd.getConditionCount()];
        }

        // Build probability array for the current ordering
        int size = ordering.size();
        double[] probArray = new double[size];
        for (int i = 0; i < size; i++) {
            probArray[i] = trueProbability != null ? trueProbability.applyAsDouble(ordering.get(i)) : 0.5;
        }

        return computeExpectedEvaluations(bdd, probArray);
    }

    int cost(Condition condition) {
        return costs.getOrDefault(condition, 1);
    }

    /**
     * Computes expected evaluations for each condition using single-pass topological traversal.
     *
     * <p>This BDD implementation guarantees that children have lower indices than parents
     * (nodes are created bottom-up). By iterating in reverse order (high to low), we process
     * parents before children, allowing single-pass O(N) probability propagation.
     */
    private static double[] computeExpectedEvaluations(Bdd bdd, double[] trueProbabilities) {
        int nodeCount = bdd.getNodeCount();
        int conditionCount = bdd.getConditionCount();
        double[] expectedEvaluations = new double[conditionCount];

        int rootRef = bdd.getRootRef();
        int rootIdx = Math.abs(rootRef) - 1;
        if (rootIdx < 0 || rootIdx >= nodeCount) {
            return expectedEvaluations;
        }

        double[] nodeReachProb = new double[nodeCount];
        nodeReachProb[rootIdx] = 1.0;

        // Iterate in reverse order: parents before children
        // (In this BDD, children have lower indices than parents)
        for (int i = nodeCount - 1; i >= 0; i--) {
            double prob = nodeReachProb[i];
            if (prob < 1e-9) {
                continue;
            }

            int varIdx = bdd.getVariable(i);

            // Accumulate expected evaluations for this condition
            if (varIdx >= 0 && varIdx < conditionCount) {
                expectedEvaluations[varIdx] += prob;
            }

            // Propagate to children
            double pTrue = (varIdx >= 0 && varIdx < trueProbabilities.length) ? trueProbabilities[varIdx] : 0.5;

            int highRef = bdd.getHigh(i);
            if (Bdd.isNodeReference(highRef)) {
                nodeReachProb[Math.abs(highRef) - 1] += prob * pTrue;
            }

            int lowRef = bdd.getLow(i);
            if (Bdd.isNodeReference(lowRef)) {
                nodeReachProb[Math.abs(lowRef) - 1] += prob * (1.0 - pTrue);
            }
        }

        return expectedEvaluations;
    }
}
