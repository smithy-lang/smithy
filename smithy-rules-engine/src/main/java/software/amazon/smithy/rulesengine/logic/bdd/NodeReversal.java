/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Reverses the node ordering in a BDD from bottom-up to top-down for better cache locality.
 *
 * <p>This transformation reverses the node array (except the terminal at index 0)
 * and updates all references throughout the BDD to maintain correctness.
 */
public final class NodeReversal implements Function<Bdd, Bdd> {

    private static final Logger LOGGER = Logger.getLogger(NodeReversal.class.getName());

    @Override
    public Bdd apply(Bdd bdd) {
        LOGGER.info("Starting BDD node reversal optimization");
        int[][] nodes = bdd.getNodes();
        int nodeCount = nodes.length;

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

        // Create new node array with reversed order
        int[][] newNodes = new int[nodeCount][];
        newNodes[0] = nodes[0].clone(); // Terminal stays at index 0

        // Add nodes in reverse order, updating their references
        int newIdx = 1;
        for (int oldIdx = nodeCount - 1; oldIdx >= 1; oldIdx--) {
            int[] oldNode = nodes[oldIdx];
            newNodes[newIdx++] = new int[] {
                    oldNode[0], // variable index stays the same
                    remapReference(oldNode[1], oldToNew), // remap high reference
                    remapReference(oldNode[2], oldToNew) // remap low reference
            };
        }

        // Remap the root reference
        int newRoot = remapReference(bdd.getRootRef(), oldToNew);

        LOGGER.info("BDD node reversal complete");

        return new Bdd(bdd.getParameters(), bdd.getConditions(), bdd.getResults(), newNodes, newRoot);
    }

    /**
     * Remaps a reference through the index mapping.
     *
     * @param ref the reference to remap
     * @param oldToNew the index mapping array
     * @return the remapped reference
     */
    private int remapReference(int ref, int[] oldToNew) {
        // Handle special cases
        if (ref == 0) {
            return 0; // Invalid reference stays invalid
        } else if (ref == 1 || ref == -1) {
            return ref; // TRUE/FALSE terminals unchanged
        } else if (ref >= Bdd.RESULT_OFFSET) {
            return ref; // Result references are not remapped
        }

        // Handle regular node references (with possible complement)
        boolean isComplemented = ref < 0;
        int absRef = isComplemented ? -ref : ref;

        // Convert from reference to index (1-based to 0-based)
        int oldIdx = absRef - 1;

        if (oldIdx >= oldToNew.length) {
            throw new IllegalStateException("Invalid reference: " + ref);
        }

        int newIdx = oldToNew[oldIdx];
        int newRef = newIdx + 1;
        return isComplemented ? -newRef : newRef;
    }
}
