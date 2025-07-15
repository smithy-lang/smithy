/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.BooleanEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.ParseUrl;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.StringEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.logic.ConditionInfo;
import software.amazon.smithy.rulesengine.logic.TestHelpers;

class DefaultOrderingStrategyTest {

    @Test
    void testIsSetComesFirst() {
        // isSet should be ordered before other conditions
        Condition isSetCond = Condition.builder().fn(TestHelpers.isSet("Region")).build();
        Condition stringEqualsCond = Condition.builder()
                .fn(StringEquals.ofExpressions(Literal.of("{Region}"), Literal.of("us-east-1")))
                .build();

        Condition[] conditions = {stringEqualsCond, isSetCond};
        Map<Condition, ConditionInfo> infos = createInfoMap(conditions);

        List<Condition> ordered = DefaultOrderingStrategy.orderConditions(conditions, infos);

        // isSet should come first
        assertEquals(isSetCond, ordered.get(0));
        assertEquals(stringEqualsCond, ordered.get(1));
    }

    @Test
    void testVariableDefiningConditionsFirst() {
        // Conditions that define variables should come before those that don't
        Condition definer = Condition.builder()
                .fn(TestHelpers.isSet("Region"))
                .result(Identifier.of("hasRegion"))
                .build();

        Condition nonDefiner = Condition.builder().fn(TestHelpers.isSet("Bucket")).build();

        Condition[] conditions = {nonDefiner, definer};
        Map<Condition, ConditionInfo> infos = createInfoMap(conditions);

        List<Condition> ordered = DefaultOrderingStrategy.orderConditions(conditions, infos);

        // Variable-defining condition should come first
        assertEquals(definer, ordered.get(0));
        assertEquals(nonDefiner, ordered.get(1));
    }

    @Test
    void testDependencyOrdering() {
        // Condition that defines a variable
        Condition definer = Condition.builder()
                .fn(TestHelpers.isSet("Region"))
                .result(Identifier.of("hasRegion"))
                .build();

        // Condition that uses the variable
        Condition user = Condition.builder()
                .fn(BooleanEquals.ofExpressions(Literal.of("{hasRegion}"), Literal.of(true)))
                .build();

        Condition[] conditions = {user, definer};
        Map<Condition, ConditionInfo> infos = createInfoMap(conditions);

        List<Condition> ordered = DefaultOrderingStrategy.orderConditions(conditions, infos);

        // Definer must come before user
        assertEquals(definer, ordered.get(0));
        assertEquals(user, ordered.get(1));
    }

    @Test
    void testComplexityOrdering() {
        // Simple condition
        Condition simple = Condition.builder().fn(TestHelpers.isSet("Region")).build();

        // Complex condition (parseURL has higher cost)
        Condition complex = Condition.builder().fn(ParseUrl.ofExpressions(Literal.of("https://example.com"))).build();

        Condition[] conditions = {complex, simple};
        Map<Condition, ConditionInfo> infos = createInfoMap(conditions);

        List<Condition> ordered = DefaultOrderingStrategy.orderConditions(conditions, infos);

        // Simple should come before complex
        assertEquals(simple, ordered.get(0));
        assertEquals(complex, ordered.get(1));
    }

    @Test
    void testCircularDependencyDetection() {
        // Create conditions with circular dependency
        // Note: This is a pathological case that shouldn't happen in practice
        Condition cond1 = Condition.builder()
                .fn(BooleanEquals.ofExpressions(Literal.of("{var2}"), Literal.of(true)))
                .result(Identifier.of("var1"))
                .build();

        Condition cond2 = Condition.builder()
                .fn(BooleanEquals.ofExpressions(Literal.of("{var1}"), Literal.of(true)))
                .result(Identifier.of("var2"))
                .build();

        Condition[] conditions = {cond1, cond2};
        Map<Condition, ConditionInfo> infos = createInfoMap(conditions);

        assertThrows(IllegalStateException.class,
                () -> DefaultOrderingStrategy.orderConditions(conditions, infos));
    }

    @Test
    void testMultiLevelDependencies() {
        // A -> B -> C dependency chain
        Condition condA = Condition.builder()
                .fn(TestHelpers.isSet("input"))
                .result(Identifier.of("var1"))
                .build();

        Condition condB = Condition.builder()
                .fn(BooleanEquals.ofExpressions(Literal.of("{var1}"), Literal.of(true)))
                .result(Identifier.of("var2"))
                .build();

        Condition condC = Condition.builder()
                .fn(BooleanEquals.ofExpressions(Literal.of("{var2}"), Literal.of(false)))
                .build();

        // Mix up the order
        Condition[] conditions = {condC, condA, condB};
        Map<Condition, ConditionInfo> infos = createInfoMap(conditions);

        List<Condition> ordered = DefaultOrderingStrategy.orderConditions(conditions, infos);

        assertEquals(condA, ordered.get(0));
        assertEquals(condB, ordered.get(1));
        assertEquals(condC, ordered.get(2));
    }

    @Test
    void testStableSortForEqualPriority() {
        // Two similar conditions with no dependencies use stable sort
        Condition cond1 = Condition.builder().fn(TestHelpers.isSet("Region")).build();
        Condition cond2 = Condition.builder().fn(TestHelpers.isSet("Bucket")).build();

        Condition[] conditions = {cond1, cond2};
        Map<Condition, ConditionInfo> infos = createInfoMap(conditions);

        List<Condition> ordered = DefaultOrderingStrategy.orderConditions(conditions, infos);

        // Order should be deterministic based on toString
        assertEquals(2, ordered.size());
        assertTrue(ordered.contains(cond1));
        assertTrue(ordered.contains(cond2));
    }

    @Test
    void testEmptyConditions() {
        Condition[] conditions = new Condition[0];
        Map<Condition, ConditionInfo> infos = new HashMap<>();

        List<Condition> ordered = DefaultOrderingStrategy.orderConditions(conditions, infos);

        assertEquals(0, ordered.size());
    }

    @Test
    void testSingleCondition() {
        Condition cond = Condition.builder().fn(TestHelpers.isSet("Region")).build();

        Condition[] conditions = {cond};
        Map<Condition, ConditionInfo> infos = createInfoMap(conditions);

        List<Condition> ordered = DefaultOrderingStrategy.orderConditions(conditions, infos);

        assertEquals(1, ordered.size());
        assertEquals(cond, ordered.get(0));
    }

    @Test
    void testIsSetDependencyForSameVariable() {
        // isSet and value check for same variable
        Condition isSet = Condition.builder().fn(TestHelpers.isSet("Region")).build();

        Condition valueCheck = Condition.builder()
                .fn(StringEquals.ofExpressions(Literal.of("{Region}"), Literal.of("us-east-1")))
                .build();

        // Put value check first to test ordering
        Condition[] conditions = {valueCheck, isSet};
        Map<Condition, ConditionInfo> infos = createInfoMap(conditions);

        List<Condition> ordered = DefaultOrderingStrategy.orderConditions(conditions, infos);

        // isSet must come before value check
        assertEquals(isSet, ordered.get(0));
        assertEquals(valueCheck, ordered.get(1));
    }

    private Map<Condition, ConditionInfo> createInfoMap(Condition... conditions) {
        Map<Condition, ConditionInfo> map = new HashMap<>();
        for (Condition cond : conditions) {
            map.put(cond, ConditionInfo.from(cond));
        }
        return map;
    }
}
