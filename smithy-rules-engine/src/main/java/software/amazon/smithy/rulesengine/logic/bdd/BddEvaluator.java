/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import software.amazon.smithy.rulesengine.logic.ConditionEvaluator;

/**
 * Simple BDD evaluator.
 */
public final class BddEvaluator {

    private final Bdd bdd;
    private final int conditionCount;

    private BddEvaluator(Bdd bdd) {
        this.bdd = bdd;
        this.conditionCount = bdd.getConditionCount();
    }

    public static BddEvaluator from(Bdd bdd) {
        return new BddEvaluator(bdd);
    }

    /**
     * Evaluates the BDD.
     *
     * @param evaluator the condition evaluator
     * @return the result index, or -1 for no match
     */
    public int evaluate(ConditionEvaluator evaluator) {
        int ref = bdd.getRootRef();

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

            int[] node = bdd.getNodes().get(nodeIdx);
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
