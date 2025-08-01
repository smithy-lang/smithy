/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.BooleanEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.logic.TestHelpers;

class OrderConstraintsTest {

    @Test
    void testIndependentConditions() {
        // Two conditions with no dependencies
        Condition cond1 = Condition.builder().fn(TestHelpers.isSet("Region")).build();
        Condition cond2 = Condition.builder().fn(TestHelpers.isSet("Bucket")).build();

        List<Condition> conditions = Arrays.asList(cond1, cond2);
        ConditionDependencyGraph graph = new ConditionDependencyGraph(conditions);
        OrderConstraints constraints = new OrderConstraints(graph, conditions);

        // Both conditions can be placed anywhere
        assertTrue(constraints.canMove(0, 1));
        assertTrue(constraints.canMove(1, 0));

        assertEquals(0, constraints.getMinValidPosition(0));
        assertEquals(1, constraints.getMaxValidPosition(0));
        assertEquals(0, constraints.getMinValidPosition(1));
        assertEquals(1, constraints.getMaxValidPosition(1));
    }

    @Test
    void testDependentConditions() {
        // cond1 defines var, cond2 uses it
        Condition cond1 = Condition.builder()
                .fn(TestHelpers.isSet("Region"))
                .result(Identifier.of("hasRegion"))
                .build();

        Condition cond2 = Condition.builder()
                .fn(BooleanEquals.ofExpressions(Literal.of("{hasRegion}"), Literal.of(true)))
                .build();

        List<Condition> conditions = Arrays.asList(cond1, cond2);
        ConditionDependencyGraph graph = new ConditionDependencyGraph(conditions);
        OrderConstraints constraints = new OrderConstraints(graph, conditions);

        // cond1 can only stay in place (cannot move past its dependent)
        assertTrue(constraints.canMove(0, 0)); // Stay in place
        assertFalse(constraints.canMove(0, 1)); // Cannot move past cond2

        // cond2 cannot move before cond1
        assertFalse(constraints.canMove(1, 0));
        assertTrue(constraints.canMove(1, 1)); // Stay in place

        assertEquals(0, constraints.getMinValidPosition(0));
        assertEquals(0, constraints.getMaxValidPosition(0)); // Must come before cond2
        assertEquals(1, constraints.getMinValidPosition(1)); // Must come after cond1
        assertEquals(1, constraints.getMaxValidPosition(1));
    }

    @Test
    void testChainedDependencies() {
        // A -> B -> C dependency chain
        Condition condA = Condition.builder().fn(TestHelpers.isSet("input")).result(Identifier.of("var1")).build();
        Condition condB = Condition.builder()
                .fn(BooleanEquals.ofExpressions(Literal.of("{var1}"), Literal.of(true)))
                .result(Identifier.of("var2"))
                .build();
        Condition condC = Condition.builder()
                .fn(BooleanEquals.ofExpressions(Literal.of("{var2}"), Literal.of(false)))
                .build();

        List<Condition> conditions = Arrays.asList(condA, condB, condC);
        ConditionDependencyGraph graph = new ConditionDependencyGraph(conditions);
        OrderConstraints constraints = new OrderConstraints(graph, conditions);

        // A can only be at position 0
        assertEquals(0, constraints.getMinValidPosition(0));
        assertEquals(0, constraints.getMaxValidPosition(0));

        // B must be between A and C
        assertEquals(1, constraints.getMinValidPosition(1));
        assertEquals(1, constraints.getMaxValidPosition(1));

        // C must be last
        assertEquals(2, constraints.getMinValidPosition(2));
        assertEquals(2, constraints.getMaxValidPosition(2));

        // No movement possible in this rigid chain
        assertFalse(constraints.canMove(0, 1));
        assertFalse(constraints.canMove(1, 0));
        assertFalse(constraints.canMove(1, 2));
        assertFalse(constraints.canMove(2, 1));
    }

    @Test
    void testCanMoveToSamePosition() {
        Condition cond = Condition.builder().fn(TestHelpers.isSet("Region")).build();
        List<Condition> conditions = Collections.singletonList(cond);
        ConditionDependencyGraph graph = new ConditionDependencyGraph(conditions);
        OrderConstraints constraints = new OrderConstraints(graph, conditions);

        // Moving to same position is always allowed
        assertTrue(constraints.canMove(0, 0));
    }

    @Test
    void testMismatchedSizes() {
        Condition cond1 = Condition.builder().fn(TestHelpers.isSet("Region")).build();
        List<Condition> graphConditions = Collections.singletonList(cond1);
        ConditionDependencyGraph graph = new ConditionDependencyGraph(graphConditions);

        // Try to create constraints with more conditions than in graph
        List<Condition> conditions = Arrays.asList(
                cond1,
                Condition.builder().fn(TestHelpers.isSet("Bucket")).build());

        assertThrows(IllegalArgumentException.class, () -> new OrderConstraints(graph, conditions));
    }
}
