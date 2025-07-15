/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;

public class ConditionReferenceTest {

    private ConditionInfo baseConditionInfo;
    private Condition simpleCondition;

    @BeforeEach
    void setUp() {
        simpleCondition = Condition.builder().fn(IsSet.ofExpressions(Literal.of("{Region}"))).build();
        baseConditionInfo = ConditionInfo.from(simpleCondition);
    }

    @Test
    void testBasicConstruction() {
        ConditionReference ref = new ConditionReference(baseConditionInfo, false);

        assertFalse(ref.isNegated());
        assertEquals(simpleCondition, ref.getCondition());
    }

    @Test
    void testNegatedConstruction() {
        ConditionReference ref = new ConditionReference(baseConditionInfo, true);

        assertTrue(ref.isNegated());
        assertEquals(simpleCondition, ref.getCondition());
    }

    @Test
    void testNegateMethod() {
        ConditionReference ref = new ConditionReference(baseConditionInfo, false);
        ConditionReference negated = ref.negate();

        assertFalse(ref.isNegated());
        assertTrue(negated.isNegated());
        assertEquals(ref.getCondition(), negated.getCondition());
    }

    @Test
    void testDoubleNegation() {
        ConditionReference ref = new ConditionReference(baseConditionInfo, false);
        ConditionReference doubleNegated = ref.negate().negate();

        assertFalse(doubleNegated.isNegated());
        assertEquals(ref.getCondition(), doubleNegated.getCondition());
    }

    @Test
    void testGetReturnVariable() {
        Condition condWithVariable = Condition.builder()
                .fn(IsSet.ofExpressions(Literal.of("{Region}")))
                .result(Identifier.of("RegionSet"))
                .build();

        ConditionInfo info = ConditionInfo.from(condWithVariable);
        ConditionReference ref = new ConditionReference(info, false);

        assertEquals("RegionSet", ref.getReturnVariable());
    }

    @Test
    void testEquals() {
        ConditionReference ref1 = new ConditionReference(baseConditionInfo, false);
        ConditionReference ref2 = new ConditionReference(baseConditionInfo, false);

        assertEquals(ref1, ref2);
    }

    @Test
    void testNotEqualsWithDifferentNegation() {
        ConditionReference ref1 = new ConditionReference(baseConditionInfo, false);
        ConditionReference ref2 = new ConditionReference(baseConditionInfo, true);

        assertNotEquals(ref1, ref2);
    }

    @Test
    void testNotEqualsWithDifferentCondition() {
        Condition otherCondition = Condition.builder().fn(IsSet.ofExpressions(Literal.of("{Bucket}"))).build();
        ConditionInfo otherInfo = ConditionInfo.from(otherCondition);
        ConditionReference ref1 = new ConditionReference(baseConditionInfo, false);
        ConditionReference ref2 = new ConditionReference(otherInfo, false);

        assertNotEquals(ref1, ref2);
    }

    @Test
    void testHashCode() {
        ConditionReference ref1 = new ConditionReference(baseConditionInfo, false);
        ConditionReference ref2 = new ConditionReference(baseConditionInfo, false);

        assertEquals(ref1.hashCode(), ref2.hashCode());
    }

    @Test
    void testHashCodeDifferentForNegated() {
        ConditionReference ref1 = new ConditionReference(baseConditionInfo, false);
        ConditionReference ref2 = new ConditionReference(baseConditionInfo, true);

        // Hash codes should be different for negated vs non-negated
        assertNotEquals(ref1.hashCode(), ref2.hashCode());
    }

    @Test
    void testToString() {
        ConditionReference ref = new ConditionReference(baseConditionInfo, false);
        String str = ref.toString();

        assertFalse(str.startsWith("!"));
        assertTrue(str.contains("isSet"));
    }

    @Test
    void testToStringNegated() {
        ConditionReference ref = new ConditionReference(baseConditionInfo, true);
        String str = ref.toString();

        assertTrue(str.startsWith("!"));
        assertTrue(str.contains("isSet"));
    }
}
