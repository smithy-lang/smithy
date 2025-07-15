/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.BooleanEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.StringEquals;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.logic.ConditionInfo;
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

        Map<Condition, ConditionInfo> conditionInfos = new HashMap<>();
        conditionInfos.put(definer, ConditionInfo.from(definer));
        conditionInfos.put(user, ConditionInfo.from(user));

        ConditionDependencyGraph graph = new ConditionDependencyGraph(conditionInfos);

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

        Map<Condition, ConditionInfo> conditionInfos = new HashMap<>();
        conditionInfos.put(isSetCondition, ConditionInfo.from(isSetCondition));
        conditionInfos.put(userCondition, ConditionInfo.from(userCondition));

        ConditionDependencyGraph graph = new ConditionDependencyGraph(conditionInfos);

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

        Map<Condition, ConditionInfo> conditionInfos = new HashMap<>();
        conditionInfos.put(definer1, ConditionInfo.from(definer1));
        conditionInfos.put(definer2, ConditionInfo.from(definer2));
        conditionInfos.put(user, ConditionInfo.from(user));

        ConditionDependencyGraph graph = new ConditionDependencyGraph(conditionInfos);

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

        Map<Condition, ConditionInfo> conditionInfos = new HashMap<>();
        conditionInfos.put(known, ConditionInfo.from(known));

        ConditionDependencyGraph graph = new ConditionDependencyGraph(conditionInfos);

        // Getting dependencies for unknown condition returns empty set
        assertTrue(graph.getDependencies(unknown).isEmpty());
    }
}
