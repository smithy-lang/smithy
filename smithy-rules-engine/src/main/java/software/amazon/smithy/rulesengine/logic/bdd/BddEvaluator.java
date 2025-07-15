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
        int resultOff = Bdd.RESULT_OFFSET;
        int ref = this.rootRef;

        while (true) {
            int abs = Math.abs(ref);
            // stop once we hit a terminal (+/-1) or a result node (|ref| >= resultOff)
            if (abs <= 1 || abs >= resultOff) {
                break;
            }

            int[] node = this.nodes[abs - 1];
            int varIdx = node[0];
            int hi = node[1];
            int lo = node[2];

            // swap branches for a complemented pointer
            if (ref < 0) {
                int tmp = hi;
                hi = lo;
                lo = tmp;
            }

            ref = evaluator.test(varIdx) ? hi : lo;
        }

        // +/-1 means no match.
        if (ref == 1 || ref == -1) {
            return -1;
        }

        int resultIdx = ref - resultOff;
        return resultIdx == 0 ? -1 : resultIdx;
    }
}
