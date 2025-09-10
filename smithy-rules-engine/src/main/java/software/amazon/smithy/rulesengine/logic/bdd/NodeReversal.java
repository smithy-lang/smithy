/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.util.function.Function;
import java.util.logging.Logger;
import software.amazon.smithy.rulesengine.traits.EndpointBddTrait;

/**
 * Reverses the node ordering in a BDD from bottom-up to top-down for better cache locality.
 *
 * <p>This transformation reverses the node array (except the terminal at index 0)
 * and updates all references throughout the BDD to maintain correctness.
 */
public final class NodeReversal implements Function<EndpointBddTrait, EndpointBddTrait> {

    private static final Logger LOGGER = Logger.getLogger(NodeReversal.class.getName());

    @Override
    public EndpointBddTrait apply(EndpointBddTrait trait) {
        Bdd reversedBdd = reverse(trait.getBdd());
        // Only rebuild the trait if the BDD actually changed
        return reversedBdd == trait.getBdd() ? trait : trait.toBuilder().bdd(reversedBdd).build();
    }

    /**
     * Reverses the node ordering in a BDD.
     *
     * @param bdd the BDD to reverse
     * @return the reversed BDD, or the original if too small to reverse
     */
    public static Bdd reverse(Bdd bdd) {
        LOGGER.info("Starting BDD node reversal optimization");
        int nodeCount = bdd.getNodeCount();

        if (nodeCount <= 2) {
            return bdd;
        }

        // Create the index mapping: old index -> new index
        // Index 0 (terminal) stays at 0
        int[] oldToNew = new int[nodeCount];
        oldToNew[0] = 0;

        // Reverse indices for non-terminal nodes
        for (int oldIdx = 1; oldIdx < nodeCount; oldIdx++) {
            int newIdx = nodeCount - oldIdx;
            oldToNew[oldIdx] = newIdx;
        }

        // Remap the root reference
        int newRoot = remapReference(bdd.getRootRef(), oldToNew);

        // Create reversed BDD using streaming constructor
        return new Bdd(newRoot, bdd.getConditionCount(), bdd.getResultCount(), nodeCount, consumer -> {
            // Terminal stays at index 0
            consumer.accept(bdd.getVariable(0), bdd.getHigh(0), bdd.getLow(0));

            // Add nodes in reverse order, updating their references
            for (int oldIdx = nodeCount - 1; oldIdx >= 1; oldIdx--) {
                int var = bdd.getVariable(oldIdx);
                int high = remapReference(bdd.getHigh(oldIdx), oldToNew);
                int low = remapReference(bdd.getLow(oldIdx), oldToNew);
                consumer.accept(var, high, low);
            }
        });
    }

    /**
     * Remaps a reference through the index mapping.
     *
     * @param ref the reference to remap
     * @param oldToNew the index mapping array
     * @return the remapped reference
     */
    private static int remapReference(int ref, int[] oldToNew) {
        // Return result references as-is.
        if (ref == 0) {
            return 0;
        } else if (ref == 1 || ref == -1) {
            return ref;
        } else if (ref >= Bdd.RESULT_OFFSET) {
            return ref;
        }

        // Handle regular node references (with possible complement)
        boolean isComplemented = ref < 0;
        int absRef = isComplemented ? -ref : ref;
        int oldIdx = absRef - 1; // convert 1-based to 0-based

        if (oldIdx >= oldToNew.length) {
            throw new IllegalStateException("Invalid reference: " + ref);
        }

        int newIdx = oldToNew[oldIdx];
        int newRef = newIdx + 1;
        return isComplemented ? -newRef : newRef;
    }
}
