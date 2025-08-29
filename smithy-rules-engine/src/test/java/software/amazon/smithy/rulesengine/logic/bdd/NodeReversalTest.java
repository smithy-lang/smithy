/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.NoMatchRule;
import software.amazon.smithy.rulesengine.traits.EndpointBddTrait;

class NodeReversalTest {

    @Test
    void testSingleNodeBdd() {
        // BDD with just terminal node
        Bdd original = new Bdd(1, 0, 0, 1, consumer -> {
            consumer.accept(-1, 1, -1); // terminal
        });

        Bdd reversed = NodeReversal.reverse(original);

        // Should be unchanged (only 1 node, reversal returns as-is for <= 2 nodes).
        assertEquals(1, reversed.getNodeCount());
        assertEquals(1, reversed.getRootRef());

        // Check terminal node
        assertEquals(-1, reversed.getVariable(0));
        assertEquals(1, reversed.getHigh(0));
        assertEquals(-1, reversed.getLow(0));
    }

    @Test
    void testComplementEdges() {
        // BDD with complement edges
        Bdd original = new Bdd(2, 2, 0, 3, consumer -> {
            consumer.accept(-1, 1, -1); // node 0: terminal
            consumer.accept(0, 3, -2); // node 1: condition 0, high to node 2, low to complement of node 1
            consumer.accept(1, 1, -1); // node 2: condition 1
        });

        Bdd reversed = NodeReversal.reverse(original);

        // Mapping: 0->0, 1->2, 2->1
        // Ref mapping: 2->3, 3->2, -2->-3
        assertEquals(3, reversed.getRootRef());

        // Check complement edge is properly remapped
        // Original node 1 is now at index 2
        assertEquals(0, reversed.getVariable(2)); // condition index unchanged
        assertEquals(2, reversed.getHigh(2)); // high ref 3 -> 2
        assertEquals(-3, reversed.getLow(2)); // complement low ref -2 -> -3
    }

    @Test
    void testResultNodes() {
        // BDD with result terminals
        Bdd original = new Bdd(2, 4, 2, 4, consumer -> {
            consumer.accept(-1, 1, -1); // node 0: terminal
            consumer.accept(0, Bdd.RESULT_OFFSET + 1, Bdd.RESULT_OFFSET); // node 1: condition 0
            consumer.accept(2, 1, -1); // node 2: result 0
            consumer.accept(3, 1, -1); // node 3: result 1
        });

        Bdd reversed = NodeReversal.reverse(original);

        assertEquals(4, reversed.getNodeCount());
        assertEquals(4, reversed.getRootRef()); // root was ref 2, now ref 4

        // Terminal stays at 0
        assertEquals(-1, reversed.getVariable(0));
        assertEquals(1, reversed.getHigh(0));
        assertEquals(-1, reversed.getLow(0));

        // Original node 3 now at index 1
        assertEquals(3, reversed.getVariable(1));
        assertEquals(1, reversed.getHigh(1));
        assertEquals(-1, reversed.getLow(1));

        // Original node 2 stays at index 2
        assertEquals(2, reversed.getVariable(2));
        assertEquals(1, reversed.getHigh(2));
        assertEquals(-1, reversed.getLow(2));

        // Original node 1 now at index 3
        assertEquals(0, reversed.getVariable(3)); // condition index unchanged
        assertEquals(Bdd.RESULT_OFFSET + 1, reversed.getHigh(3)); // result references unchanged
        assertEquals(Bdd.RESULT_OFFSET, reversed.getLow(3)); // result references unchanged
    }

    @Test
    void testFourNodeExample() {
        // Simple 4-node example to verify reference mapping
        Bdd original = new Bdd(2, 3, 0, 4, consumer -> {
            consumer.accept(-1, 1, -1); // node 0: terminal
            consumer.accept(0, 3, 4); // node 1: points to nodes 2 and 3
            consumer.accept(1, 1, -1); // node 2:
            consumer.accept(2, 1, -1); // node 3:
        });

        Bdd reversed = NodeReversal.reverse(original);

        // Mapping: 0->0, 1->3, 2->2, 3->1
        // Ref mapping: 2->4, 3->3, 4->2
        assertEquals(4, reversed.getRootRef()); // root ref 2 -> 4

        // Check node at index 3 (originally node 1)
        assertEquals(0, reversed.getVariable(3)); // original node 1's variable
        assertEquals(3, reversed.getHigh(3)); // ref 3 stays 3
        assertEquals(2, reversed.getLow(3)); // ref 4 -> 2
    }

    @Test
    void testImmutability() {
        // Ensure original BDD is not modified
        Bdd original = new Bdd(2, 2, 0, 3, consumer -> {
            consumer.accept(-1, 1, -1); // node 0
            consumer.accept(0, 3, -1); // node 1
            consumer.accept(1, 1, -1); // node 2
        });

        // Get original values for comparison
        int originalNodeCount = original.getNodeCount();
        int originalRootRef = original.getRootRef();

        // Store original node values
        int[] originalNodeValues = new int[original.getNodeCount() * 3];
        for (int i = 0; i < original.getNodeCount(); i++) {
            originalNodeValues[i * 3] = original.getVariable(i);
            originalNodeValues[i * 3 + 1] = original.getHigh(i);
            originalNodeValues[i * 3 + 2] = original.getLow(i);
        }

        Bdd reversed = NodeReversal.reverse(original);

        // Verify original is unchanged
        assertEquals(originalNodeCount, original.getNodeCount());
        assertEquals(originalRootRef, original.getRootRef());

        // Check original node values haven't changed
        for (int i = 0; i < original.getNodeCount(); i++) {
            assertEquals(originalNodeValues[i * 3], original.getVariable(i));
            assertEquals(originalNodeValues[i * 3 + 1], original.getHigh(i));
            assertEquals(originalNodeValues[i * 3 + 2], original.getLow(i));
        }

        // Ensure reversed is a different object
        assertNotSame(original, reversed);
    }

    @Test
    void testTwoNodeBdd() {
        // Test edge case with exactly 2 nodes
        Bdd original = new Bdd(2, 1, 0, 2, consumer -> {
            consumer.accept(-1, 1, -1); // node 0: terminal
            consumer.accept(0, 1, -1); // node 1: simple condition
        });

        Bdd reversed = NodeReversal.reverse(original);

        // Should be unchanged (reversal returns as-is for <= 2 nodes)
        assertEquals(2, reversed.getNodeCount());
        assertEquals(2, reversed.getRootRef());

        // Check nodes are unchanged
        assertEquals(-1, reversed.getVariable(0));
        assertEquals(1, reversed.getHigh(0));
        assertEquals(-1, reversed.getLow(0));

        assertEquals(0, reversed.getVariable(1));
        assertEquals(1, reversed.getHigh(1));
        assertEquals(-1, reversed.getLow(1));
    }

    @Test
    void testBddTraitReversalReturnsOriginalForSmallBdd() {
        // Test that small BDDs return the original trait unchanged
        NodeReversal reversal = new NodeReversal();

        // Create a BddTrait with a 2-node BDD
        Bdd bdd = new Bdd(2, 1, 1, 2, consumer -> {
            consumer.accept(-1, 1, -1); // node 0: terminal
            consumer.accept(0, 1, -1); // node 1: simple condition
        });

        EndpointBddTrait originalTrait = EndpointBddTrait.builder()
                .parameters(Parameters.builder().build())
                .conditions(new ArrayList<>())
                .results(Collections.singletonList(NoMatchRule.INSTANCE))
                .bdd(bdd)
                .build();

        EndpointBddTrait reversedTrait = reversal.apply(originalTrait);

        // Should return the exact same trait object for small BDDs
        assertSame(originalTrait, reversedTrait);
    }
}
