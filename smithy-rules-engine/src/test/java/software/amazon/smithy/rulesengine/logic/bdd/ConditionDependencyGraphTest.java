/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.BooleanEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.StringEquals;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.logic.TestHelpers;

class ConditionDependencyGraphTest {

    @Test
    void testBasicVariableDependency() {
        // Condition that defines a variable
        Condition definer = Condition.builder()
                .fn(TestHelpers.isSet("Region"))
                .result(Identifier.of("hasRegion"))
                .build();

        // Condition that uses the variable
        Condition user = Condition.builder()
                .fn(BooleanEquals.ofExpressions(Expression.of("{hasRegion}"), Expression.of(true)))
                .build();

        List<Condition> conditions = Arrays.asList(definer, user);
        ConditionDependencyGraph graph = new ConditionDependencyGraph(conditions);

        // Definer has no dependencies
        assertTrue(graph.getDependencies(definer).isEmpty());

        // User depends on definer
        Set<Condition> userDeps = graph.getDependencies(user);
        assertEquals(1, userDeps.size());
        assertTrue(userDeps.contains(definer));
    }

    @Test
    void testIsSetDependencyForNonIsSetCondition() {
        // isSet condition for a variable
        Condition isSetCondition = Condition.builder().fn(TestHelpers.isSet("Region")).build();
        // Non-isSet condition using the same variable
        Condition userCondition = Condition.builder()
                .fn(StringEquals.ofExpressions(Expression.of("{Region}"), Expression.of("us-east-1")))
                .build();

        List<Condition> conditions = Arrays.asList(isSetCondition, userCondition);
        ConditionDependencyGraph graph = new ConditionDependencyGraph(conditions);

        // Non-isSet condition depends on isSet for undefined variables
        Set<Condition> userDeps = graph.getDependencies(userCondition);
        assertEquals(1, userDeps.size());
        assertTrue(userDeps.contains(isSetCondition));
    }

    @Test
    void testMultipleDependencies() {
        // Define two variables
        Condition definer1 = Condition.builder()
                .fn(TestHelpers.isSet("Region"))
                .result(Identifier.of("hasRegion"))
                .build();

        Condition definer2 = Condition.builder()
                .fn(TestHelpers.isSet("Bucket"))
                .result(Identifier.of("hasBucket"))
                .build();

        // Use both variables
        Condition user = Condition.builder()
                .fn(BooleanEquals.ofExpressions(
                        BooleanEquals.ofExpressions(Expression.of("{hasRegion}"), Expression.of(true)),
                        BooleanEquals.ofExpressions(Expression.of("{hasBucket}"), Expression.of(true))))
                .build();

        List<Condition> conditions = Arrays.asList(definer1, definer2, user);
        ConditionDependencyGraph graph = new ConditionDependencyGraph(conditions);

        // User depends on both definers
        Set<Condition> userDeps = graph.getDependencies(user);
        assertEquals(2, userDeps.size());
        assertTrue(userDeps.contains(definer1));
        assertTrue(userDeps.contains(definer2));
    }

    @Test
    void testUnknownConditionReturnsEmptyDependencies() {
        Condition known = Condition.builder().fn(TestHelpers.isSet("Region")).build();
        Condition unknown = Condition.builder().fn(TestHelpers.isSet("Bucket")).build();

        List<Condition> conditions = Arrays.asList(known);
        ConditionDependencyGraph graph = new ConditionDependencyGraph(conditions);

        // Getting dependencies for unknown condition returns empty set
        assertTrue(graph.getDependencies(unknown).isEmpty());
    }

    @Test
    void testGraphSize() {
        Condition cond1 = Condition.builder().fn(TestHelpers.isSet("A")).build();
        Condition cond2 = Condition.builder().fn(TestHelpers.isSet("B")).build();
        Condition cond3 = Condition.builder().fn(TestHelpers.isSet("C")).build();

        List<Condition> conditions = Arrays.asList(cond1, cond2, cond3);
        ConditionDependencyGraph graph = new ConditionDependencyGraph(conditions);

        assertEquals(3, graph.size());
    }
}
