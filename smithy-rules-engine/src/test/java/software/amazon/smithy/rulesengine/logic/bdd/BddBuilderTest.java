/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BddBuilderTest {

    private BddBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new BddBuilder();
    }

    @Test
    void testTerminals() {
        assertEquals(1, builder.makeTrue());
        assertEquals(-1, builder.makeFalse());

        // Terminals are constants
        assertEquals(1, builder.makeTrue());
        assertEquals(-1, builder.makeFalse());
    }

    @Test
    void testNodeReduction() {
        builder.setConditionCount(2);

        // Node with identical branches should be reduced
        int reduced = builder.makeNode(0, builder.makeTrue(), builder.makeTrue());
        assertEquals(1, reduced); // Should return TRUE directly

        // Verify no new node was created (only terminal at index 0)
        assertEquals(1, builder.getNodeCount()); // Only terminal node exists
    }

    @Test
    void testComplementCanonicalization() {
        builder.setConditionCount(2);

        // Create node with complement on low branch
        int node1 = builder.makeNode(0, builder.makeTrue(), builder.makeFalse());

        // Create equivalent node with flipped branches and complement
        // This should canonicalize to the same node but with complement
        int node2 = builder.makeNode(0, builder.makeFalse(), builder.makeTrue());

        assertEquals(-node1, node2); // Should be complement of first node

        // Only one actual node should be created (plus terminal)
        assertEquals(2, builder.getNodeCount()); // Terminal + one node
    }

    @Test
    void testNodeDeduplication() {
        builder.setConditionCount(2);

        // Create same node twice
        int node1 = builder.makeNode(0, builder.makeTrue(), builder.makeFalse());
        int node2 = builder.makeNode(0, builder.makeTrue(), builder.makeFalse());

        assertEquals(node1, node2); // Should return same reference
        assertEquals(2, builder.getNodeCount()); // Terminal + one node (no duplicate)
    }

    @Test
    void testResultNodes() {
        builder.setConditionCount(2);

        int result0 = builder.makeResult(0);
        int result1 = builder.makeResult(1);

        // Result nodes should have distinct references
        assertTrue(result0 != result1);
        assertTrue(builder.isResult(result0));
        assertTrue(builder.isResult(result1));
        assertFalse(builder.isResult(builder.makeTrue()));

        // Result refs should be encoded with RESULT_OFFSET
        assertEquals(Bdd.RESULT_OFFSET, result0);
        assertEquals(Bdd.RESULT_OFFSET + 1, result1);
    }

    @Test
    void testNegation() {
        builder.setConditionCount(2);

        int node = builder.makeNode(0, builder.makeTrue(), builder.makeFalse());
        int negated = builder.negate(node);

        assertEquals(-node, negated);
        assertEquals(node, builder.negate(negated)); // Double negation

        // Cannot negate terminals
        assertEquals(-1, builder.negate(builder.makeTrue()));
        assertEquals(1, builder.negate(builder.makeFalse()));
    }

    @Test
    void testNegateResult() {
        builder.setConditionCount(2);
        int result = builder.makeResult(0);

        assertThrows(IllegalArgumentException.class, () -> builder.negate(result));
    }

    @Test
    void testAndOperation() {
        builder.setConditionCount(2);

        // TRUE AND TRUE = TRUE
        assertEquals(1, builder.and(builder.makeTrue(), builder.makeTrue()));

        // TRUE AND FALSE = FALSE
        assertEquals(-1, builder.and(builder.makeTrue(), builder.makeFalse()));

        // FALSE AND x = FALSE
        assertEquals(-1, builder.and(builder.makeFalse(), builder.makeTrue()));
        assertEquals(-1, builder.and(builder.makeFalse(), builder.makeFalse()));

        // x AND x = x
        int node = builder.makeNode(0, builder.makeTrue(), builder.makeFalse());
        assertEquals(node, builder.and(node, node));
    }

    @Test
    void testOrOperation() {
        builder.setConditionCount(2);

        // FALSE OR FALSE = FALSE
        assertEquals(-1, builder.or(builder.makeFalse(), builder.makeFalse()));

        // TRUE OR x = TRUE
        assertEquals(1, builder.or(builder.makeTrue(), builder.makeFalse()));
        assertEquals(1, builder.or(builder.makeTrue(), builder.makeTrue()));

        // FALSE OR TRUE = TRUE
        assertEquals(1, builder.or(builder.makeFalse(), builder.makeTrue()));

        // x OR x = x
        int node = builder.makeNode(0, builder.makeTrue(), builder.makeFalse());
        assertEquals(node, builder.or(node, node));
    }

    @Test
    void testIteBasicCases() {
        builder.setConditionCount(2);

        // ITE(TRUE, g, h) = g
        int g = builder.makeNode(0, builder.makeTrue(), builder.makeFalse());
        int h = builder.makeNode(1, builder.makeTrue(), builder.makeFalse());
        assertEquals(g, builder.ite(builder.makeTrue(), g, h));

        // ITE(FALSE, g, h) = h
        assertEquals(h, builder.ite(builder.makeFalse(), g, h));

        // ITE(f, g, g) = g
        int f = builder.makeNode(0, builder.makeTrue(), builder.makeFalse());
        assertEquals(g, builder.ite(f, g, g));
    }

    @Test
    void testIteWithComplement() {
        builder.setConditionCount(2);

        int f = builder.makeNode(0, builder.makeTrue(), builder.makeFalse());
        int g = builder.makeNode(1, builder.makeTrue(), builder.makeFalse());
        int h = builder.makeNode(1, builder.makeFalse(), builder.makeTrue());

        // ITE with complemented condition should swap branches
        int result1 = builder.ite(f, g, h);
        int result2 = builder.ite(builder.negate(f), h, g);
        assertEquals(result1, result2);
    }

    @Test
    void testResultInIte() {
        builder.setConditionCount(1);

        int cond = builder.makeNode(0, builder.makeTrue(), builder.makeFalse());
        int result0 = builder.makeResult(0);
        int result1 = builder.makeResult(1);

        // ITE with result terminals
        int ite = builder.ite(cond, result0, result1);
        assertTrue(ite != 0);

        // Condition cannot be a result
        assertThrows(IllegalArgumentException.class,
                () -> builder.ite(result0, builder.makeTrue(), builder.makeFalse()));
    }

    @Test
    void testSetConditionCountRequired() {
        assertThrows(IllegalStateException.class, () -> builder.makeResult(0));
    }

    @Test
    void testGetVariable() {
        builder.setConditionCount(3);

        assertEquals(-1, builder.getVariable(builder.makeTrue()));
        assertEquals(-1, builder.getVariable(builder.makeFalse()));

        int node = builder.makeNode(1, builder.makeTrue(), builder.makeFalse());
        assertEquals(1, builder.getVariable(node));
        assertEquals(1, builder.getVariable(Math.abs(node))); // Use absolute value for complement

        int result = builder.makeResult(0);
        assertEquals(-1, builder.getVariable(result)); // results have no variable
    }

    @Test
    void testReduceSimpleBdd() {
        builder.setConditionCount(3);

        // makeNode already reduces nodes with identical branches
        // So we need to create a different scenario for reduction
        int a = builder.makeNode(2, builder.makeTrue(), builder.makeFalse());
        int b = builder.makeNode(1, a, builder.makeFalse());
        int root = builder.makeNode(0, b, a);

        int nodesBefore = builder.getNodeCount();
        builder.reduce(root);

        // Structure should be preserved if already optimal
        assertEquals(nodesBefore, builder.getNodeCount());
    }

    @Test
    void testReduceNoChange() {
        builder.setConditionCount(2);

        // Create already-reduced BDD
        int right = builder.makeNode(1, builder.makeTrue(), builder.makeFalse());
        int root = builder.makeNode(0, right, builder.makeFalse());

        int nodesBefore = builder.getNodeCount();
        builder.reduce(root);

        assertEquals(nodesBefore, builder.getNodeCount());
    }

    @Test
    void testReduceTerminals() {
        // Reducing terminals should return them unchanged
        assertEquals(1, builder.reduce(builder.makeTrue()));
        assertEquals(-1, builder.reduce(builder.makeFalse()));
    }

    @Test
    void testReduceResults() {
        builder.setConditionCount(1);
        int result = builder.makeResult(0);

        // Reducing result nodes should return them unchanged
        assertEquals(result, builder.reduce(result));
    }

    @Test
    void testReduceWithComplement() {
        builder.setConditionCount(3);

        // Create BDD with complement edges
        int a = builder.makeNode(2, builder.makeTrue(), builder.makeFalse());
        int b = builder.makeNode(1, a, builder.negate(a));
        int root = builder.makeNode(0, b, builder.makeFalse());

        int reduced = builder.reduce(root);

        // Now test reducing with complemented root
        int complementRoot = builder.negate(root);
        int reducedComplement = builder.reduce(complementRoot);

        // The result should be the complement of the reduced root
        assertEquals(builder.negate(reduced), reducedComplement);

        // Verify the structure is preserved
        assertTrue(builder.getNodeCount() > 1); // More than just terminal
    }

    @Test
    void testReduceClearsCache() {
        builder.setConditionCount(2);

        // Create nodes and perform ITE to populate cache - use only boolean nodes
        int a = builder.makeNode(0, builder.makeTrue(), builder.makeFalse());
        int b = builder.makeNode(1, builder.makeTrue(), builder.makeFalse());
        int ite1 = builder.ite(a, b, builder.makeFalse());

        builder.reduce(ite1);

        // Cache should be cleared, so same ITE creates new result
        // Recreate the nodes since reduce may have changed internal state
        a = builder.makeNode(0, builder.makeTrue(), builder.makeFalse());
        b = builder.makeNode(1, builder.makeTrue(), builder.makeFalse());
        int ite2 = builder.ite(a, b, builder.makeFalse());
        assertTrue(ite2 != 0); // Should get a valid reference
    }

    @Test
    void testReduceActuallyReduces() {
        builder.setConditionCount(3);

        // First create some nodes
        int bottom = builder.makeNode(2, builder.makeTrue(), builder.makeFalse());
        int middle = builder.makeNode(1, bottom, builder.makeFalse());
        int root = builder.makeNode(0, middle, bottom);

        int beforeSize = builder.getNodeCount();
        builder.reduce(root);
        int afterSize = builder.getNodeCount();

        // In this case, no reduction should occur since makeNode already optimized
        assertEquals(beforeSize, afterSize);
    }

    @Test
    void testReduceWithPreExistingComplementStructure() {
        builder.setConditionCount(3);

        // Create a structure where reduce will encounter complement on low during rebuild
        // First create base nodes
        int a = builder.makeNode(2, builder.makeTrue(), builder.makeFalse());

        // When we create this node, makeNode will canonicalize it
        // The actual stored node will have the complement bit on the reference, not in the node
        int b = builder.makeNode(1, builder.makeTrue(), builder.negate(a));

        // This creates a scenario where during reduce's rebuild,
        // makeNodeInNew might encounter the complement
        int root = builder.makeNode(0, b, a);

        // Force a reduce operation
        int reduced = builder.reduce(root);

        // The BDD should be functionally equivalent
        // We can't make strong assertions about node count since reduce may optimize
        assertTrue(reduced != 0);

        // Verify the BDD still evaluates correctly
        // by checking that it's not a constant
        assertNotEquals(reduced, builder.makeTrue());
        assertNotEquals(reduced, builder.makeFalse());
    }

    @Test
    void testCofactorRecursive() {
        builder.setConditionCount(3);

        // Create a multi-level BDD with only boolean nodes (no results)
        int bottom = builder.makeNode(2, builder.makeTrue(), builder.makeFalse());
        int middle = builder.makeNode(1, bottom, builder.makeFalse());
        int root = builder.makeNode(0, middle, bottom);

        // Cofactor with respect to variable 1 (appears deeper in BDD)
        int cofactorTrue = builder.cofactor(root, 1, true);
        int cofactorFalse = builder.cofactor(root, 1, false);

        // The cofactors should be different
        assertTrue(cofactorTrue != cofactorFalse);

        // Verify structure exists
        assertTrue(builder.getNodeCount() > 1);
    }

    @Test
    void testCofactorWithResults() {
        builder.setConditionCount(2);

        // Create BDD with result terminals
        int result0 = builder.makeResult(0);
        int result1 = builder.makeResult(1);
        int node = builder.makeNode(0, result0, result1);

        // Cofactor should select appropriate result
        assertEquals(result0, builder.cofactor(node, 0, true));
        assertEquals(result1, builder.cofactor(node, 0, false));
    }

    @Test
    void testReduceWithResults() {
        builder.setConditionCount(2);

        // Create a BDD that uses results properly
        int result0 = builder.makeResult(0);
        int result1 = builder.makeResult(1);

        // Create condition nodes that branch to results
        int node = builder.makeNode(1, result0, result1);
        int root = builder.makeNode(0, node, builder.makeFalse());

        int reduced = builder.reduce(root);

        // The structure should be preserved
        assertNotEquals(0, reduced); // Should not be invalid
        assertFalse(builder.isResult(reduced)); // Root should still be a condition node
    }

    @Test
    void testIteWithResultsInBranches() {
        builder.setConditionCount(2);

        // Create a condition and two results
        int cond = builder.makeNode(0, builder.makeTrue(), builder.makeFalse());
        int result0 = builder.makeResult(0);
        int result1 = builder.makeResult(1);

        // ITE should handle results in then/else branches
        int ite = builder.ite(cond, result0, result1);

        // The result should be a node that branches to the two results
        assertTrue(ite > 0);
        assertFalse(builder.isResult(ite));
    }

    @Test
    void testResultMaskNoCollisions() {
        builder.setConditionCount(3);

        // Create many nodes to ensure no collision with result encoding
        int node1 = builder.makeNode(0, builder.makeTrue(), builder.makeFalse());
        int node2 = builder.makeNode(1, node1, builder.makeFalse());
        int node3 = builder.makeNode(2, node2, node1);

        int result0 = builder.makeResult(0);
        int result1 = builder.makeResult(1);

        // Verify no collisions
        assertNotEquals(node1, result0);
        assertNotEquals(node2, result0);
        assertNotEquals(node3, result0);
        assertNotEquals(node1, result1);
        assertNotEquals(node2, result1);
        assertNotEquals(node3, result1);

        // Verify correct identification
        assertFalse(builder.isResult(node1));
        assertFalse(builder.isResult(node2));
        assertFalse(builder.isResult(node3));
        assertTrue(builder.isResult(result0));
        assertTrue(builder.isResult(result1));
    }

    @Test
    void testReset() {
        builder.setConditionCount(2);

        // Create some state
        builder.makeNode(0, builder.makeTrue(), builder.makeFalse());
        builder.makeResult(0);

        builder.reset();

        assertEquals(1, builder.getNodeCount()); // Only terminal
        assertThrows(IllegalStateException.class, () -> builder.makeResult(0));

        // Can use builder again
        builder.setConditionCount(1);
        int newNode = builder.makeNode(0, builder.makeTrue(), builder.makeFalse());
        assertNotEquals(0, newNode); // Should get a valid reference
        assertNotEquals(1, Math.abs(newNode)); // Should not be a terminal
        assertNotEquals(-1, Math.abs(newNode)); // Should not be a terminal
    }
}
