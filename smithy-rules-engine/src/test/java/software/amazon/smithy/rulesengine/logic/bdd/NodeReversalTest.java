/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.logic.TestHelpers;

class NodeReversalTest {

    @Test
    void testSingleNodeBdd() {
        // BDD with just terminal node
        int[][] nodes = new int[][] {
                {-1, 1, -1} // terminal
        };

        Bdd original = new Bdd(
                Parameters.builder().build(),
                new ArrayList<>(),
                new ArrayList<>(),
                nodes,
                1 // root is TRUE
        );

        NodeReversal reversal = new NodeReversal();
        Bdd reversed = reversal.apply(original);

        // Should be unchanged (2 nodes returns as-is).
        assertEquals(1, reversed.getNodes().length);
        assertEquals(1, reversed.getRootRef());
        assertArrayEquals(new int[] {-1, 1, -1}, reversed.getNodes()[0]);
    }

    @Test
    void testComplementEdges() {
        // BDD with complement edges
        int[][] nodes = new int[][] {
                {-1, 1, -1}, // terminal
                {0, 3, -2}, // condition 0, high to node 2, low to complement of node 1
                {1, 1, -1} // condition 1
        };

        Bdd original = new Bdd(
                Parameters.builder().build(),
                Arrays.asList(
                        Condition.builder().fn(TestHelpers.isSet("Region")).build(),
                        Condition.builder().fn(TestHelpers.isSet("Bucket")).build()),
                new ArrayList<>(),
                nodes,
                2 // root points to node 1
        );

        NodeReversal reversal = new NodeReversal();
        Bdd reversed = reversal.apply(original);

        // Mapping: 0->0, 1->2, 2->1
        // Ref mapping: 2->3, 3->2, -2->-3
        assertEquals(3, reversed.getRootRef());

        // Check complement edge is properly remapped
        int[] reversedNode2 = reversed.getNodes()[2];
        assertEquals(0, reversedNode2[0]); // condition index unchanged
        assertEquals(2, reversedNode2[1]); // high ref 3 -> 2
        assertEquals(-3, reversedNode2[2]); // complement low ref -2 -> -3
    }

    @Test
    void testResultNodes() {
        // BDD with result terminals
        int[][] nodes = new int[][] {
                {-1, 1, -1}, // terminal
                {0, Bdd.RESULT_OFFSET + 1, Bdd.RESULT_OFFSET}, // condition 0 at index 1
                {2, 1, -1}, // result 0 at index 2
                {3, 1, -1} // result 1 at index 3
        };

        List<Rule> results = Arrays.asList(
                EndpointRule.builder().endpoint(TestHelpers.endpoint("https://example.com")),
                ErrorRule.builder().error("Error occurred"));

        Bdd original = new Bdd(
                Parameters.builder().build(),
                Arrays.asList(
                        Condition.builder().fn(TestHelpers.isSet("Region")).build(),
                        Condition.builder().fn(TestHelpers.isSet("Bucket")).build()),
                results,
                nodes,
                2 // root points to node 1
        );

        NodeReversal reversal = new NodeReversal();
        Bdd reversed = reversal.apply(original);

        assertEquals(4, reversed.getNodes().length);
        assertEquals(4, reversed.getRootRef()); // root was ref 2, now ref 4

        // Terminal stays at 0
        assertArrayEquals(new int[] {-1, 1, -1}, reversed.getNodes()[0]);

        // Original node 3 now at index 1
        assertArrayEquals(new int[] {3, 1, -1}, reversed.getNodes()[1]);

        // Original node 2 stays at index 2
        assertArrayEquals(new int[] {2, 1, -1}, reversed.getNodes()[2]);

        // Original node 1 now at index 3
        int[] conditionNode = reversed.getNodes()[3];
        assertEquals(0, conditionNode[0]); // condition index unchanged
        assertEquals(Bdd.RESULT_OFFSET + 1, conditionNode[1]); // result references unchanged
        assertEquals(Bdd.RESULT_OFFSET, conditionNode[2]); // result references unchanged
    }

    @Test
    void testFourNodeExample() {
        // Simple 4-node example to verify reference mapping
        int[][] nodes = new int[][] {
                {-1, 1, -1}, // 0: terminal
                {0, 3, 4}, // 1: points to nodes 2 and 3
                {1, 1, -1}, // 2:
                {2, 1, -1} // 3:
        };

        Bdd original = new Bdd(
                Parameters.builder().build(),
                Arrays.asList(
                        Condition.builder().fn(TestHelpers.isSet("A")).build(),
                        Condition.builder().fn(TestHelpers.isSet("B")).build(),
                        Condition.builder().fn(TestHelpers.isSet("C")).build()),
                new ArrayList<>(),
                nodes,
                2 // root points to node 1
        );

        NodeReversal reversal = new NodeReversal();
        Bdd reversed = reversal.apply(original);

        // Mapping: 0->0, 1->3, 2->2, 3->1
        // Ref mapping: 2->4, 3->3, 4->2
        assertEquals(4, reversed.getRootRef()); // root ref 2 -> 4

        int[] nodeAtIndex3 = reversed.getNodes()[3]; // original node 1
        assertEquals(0, nodeAtIndex3[0]);
        assertEquals(3, nodeAtIndex3[1]); // ref 3 stays 3
        assertEquals(2, nodeAtIndex3[2]); // ref 4 -> 2
    }

    @Test
    void testImmutability() {
        // Ensure original BDD is not modified
        int[][] originalNodes = new int[][] {
                {-1, 1, -1},
                {0, 3, -1},
                {1, 1, -1}
        };

        Bdd original = new Bdd(
                Parameters.builder().build(),
                Arrays.asList(
                        Condition.builder().fn(TestHelpers.isSet("Region")).build(),
                        Condition.builder().fn(TestHelpers.isSet("Bucket")).build()),
                new ArrayList<>(),
                originalNodes,
                2);

        // Clone original node arrays for comparison
        int[][] originalNodesCopy = new int[original.getNodes().length][];
        for (int i = 0; i < original.getNodes().length; i++) {
            originalNodesCopy[i] = original.getNodes()[i].clone();
        }

        NodeReversal reversal = new NodeReversal();
        Bdd reversed = reversal.apply(original);

        // Verify original is unchanged
        assertEquals(originalNodesCopy.length, original.getNodes().length);
        for (int i = 0; i < originalNodesCopy.length; i++) {
            assertArrayEquals(originalNodesCopy[i], original.getNodes()[i]);
        }

        assertNotSame(original.getNodes(), reversed.getNodes());
    }
}
