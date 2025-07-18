/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import software.amazon.smithy.rulesengine.logic.ConditionEvaluator;

/**
 * Simple BDD evaluator that works directly with BDD nodes.
 */
public final class BddEvaluator {

    private final int[][] nodes;
    private final int rootRef;
    private final int conditionCount;

    private BddEvaluator(int[][] nodes, int rootRef, int conditionCount) {
        this.nodes = nodes;
        this.rootRef = rootRef;
        this.conditionCount = conditionCount;
    }

    /**
     * Create evaluator from a Bdd object.
     *
     * @param bdd the BDD
     * @return the evaluator
     */
    public static BddEvaluator from(Bdd bdd) {
        return from(bdd.getNodes(), bdd.getRootRef(), bdd.getConditionCount());
    }

    /**
     * Create evaluator from BDD data.
     *
     * @param nodes BDD nodes array
     * @param rootRef root reference
     * @param conditionCount number of conditions
     * @return the evaluator
     */
    public static BddEvaluator from(int[][] nodes, int rootRef, int conditionCount) {
        return new BddEvaluator(nodes, rootRef, conditionCount);
    }

    /**
     * Evaluates the BDD.
     *
     * @param evaluator the condition evaluator
     * @return the result index, or -1 for no match
     */
    public int evaluate(ConditionEvaluator evaluator) {
        int ref = rootRef;

        while (true) {
            // Handle terminals
            if (ref == 1 || ref == -1) {
                return -1;
            }

            // Check if this is a result reference
            if (ref >= Bdd.RESULT_OFFSET) {
                int resultIdx = ref - Bdd.RESULT_OFFSET;
                return resultIdx == 0 ? -1 : resultIdx;
            }

            // Get absolute reference and track complement
            boolean complemented = ref < 0;
            int nodeIdx = (complemented ? -ref : ref) - 1;

            int[] node = nodes[nodeIdx];
            int varIdx = node[0];

            // Evaluate condition and follow appropriate branch
            boolean condResult = evaluator.test(varIdx);
            if (complemented) {
                condResult = !condResult;
            }

            ref = condResult ? node[1] : node[2];
        }
    }
}
