/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.BooleanEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.GetAttr;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.Not;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.StringEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.BooleanLiteral;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.StringLiteral;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;

public class ConditionInfoImplTest {

    @Test
    void testSimpleIsSetCondition() {
        Condition condition = Condition.builder()
                .fn(IsSet.ofExpressions(Literal.of("{Region}")))
                .build();

        ConditionInfo info = ConditionInfo.from(condition);

        assertEquals(condition, info.getCondition());

        assertEquals(4, info.getComplexity());
        assertEquals(1, info.getReferences().size());
        assertTrue(info.getReferences().contains("Region"));
        assertNull(info.getReturnVariable());
    }

    @Test
    void testConditionWithVariableBinding() {
        Condition condition = Condition.builder()
                .fn(IsSet.ofExpressions(Literal.of("{Region}")))
                .result(Identifier.of("RegionExists"))
                .build();

        ConditionInfo info = ConditionInfo.from(condition);

        assertEquals("RegionExists", info.getReturnVariable());
    }

    @Test
    void testComplexNestedCondition() {
        // Test nested function calls
        Condition condition = Condition.builder()
                .fn(Not.ofExpressions(
                        BooleanEquals.ofExpressions(
                                IsSet.ofExpressions(Literal.of("{Region}")),
                                BooleanLiteral.of(true))))
                .build();

        ConditionInfo info = ConditionInfo.from(condition);

        assertEquals(11, info.getComplexity());
        assertEquals(1, info.getReferences().size());
        assertTrue(info.getReferences().contains("Region"));
    }

    @Test
    void testTemplateStringComplexity() {
        Condition condition = Condition.builder()
                .fn(StringEquals.ofExpressions(
                        Literal.of("{Endpoint}"),
                        StringLiteral.of("https://{Service}.{Region}.amazonaws.com")))
                .build();

        ConditionInfo info = ConditionInfo.from(condition);

        assertTrue(info.getComplexity() > StringEquals.getDefinition().getCostHeuristic());
        assertEquals(3, info.getReferences().size());
        assertTrue(info.getReferences().contains("Endpoint"));
        assertTrue(info.getReferences().contains("Service"));
        assertTrue(info.getReferences().contains("Region"));
    }

    @Test
    void testGetAttrNestedComplexity() {
        Condition condition = Condition.builder()
                .fn(GetAttr.ofExpressions(GetAttr.ofExpressions(Literal.of("{ComplexObject}"), "nested"), "value"))
                .build();

        ConditionInfo info = ConditionInfo.from(condition);

        assertEquals(8, info.getComplexity());
        assertEquals(1, info.getReferences().size());
        assertTrue(info.getReferences().contains("ComplexObject"));
    }

    @Test
    void testEquals() {
        Condition condition1 = Condition.builder().fn(IsSet.ofExpressions(Literal.of("{Region}"))).build();
        Condition condition2 = Condition.builder().fn(IsSet.ofExpressions(Literal.of("{Region}"))).build();

        ConditionInfo info1 = ConditionInfo.from(condition1);
        ConditionInfo info2 = ConditionInfo.from(condition2);

        assertEquals(info1, info2);
        assertEquals(info1.hashCode(), info2.hashCode());
    }

    @Test
    void testToString() {
        Condition condition = Condition.builder()
                .fn(IsSet.ofExpressions(Literal.of("{Region}")))
                .result(Identifier.of("RegionExists"))
                .build();

        ConditionInfo info = ConditionInfo.from(condition);

        String str = info.toString();
        assertTrue(str.contains("isSet"));
        assertTrue(str.contains("Region"));
        assertTrue(str.contains("RegionExists"));
    }
}
