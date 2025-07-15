/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.evaluation.RuleEvaluator;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;

class RuleBasedConditionEvaluatorTest {

    @Test
    void testEvaluatesConditions() {
        Condition cond1 = Condition.builder().fn(TestHelpers.isSet("param1")).build();
        Condition cond2 = Condition.builder().fn(TestHelpers.isSet("param2")).build();
        Condition[] conditions = {cond1, cond2};

        // Create a mock evaluator that returns true for first condition, false for second
        RuleEvaluator mockEvaluator = new RuleEvaluator() {
            @Override
            public Value evaluateCondition(Condition condition) {
                if (condition == cond1) {
                    return Value.booleanValue(true);
                } else {
                    return Value.booleanValue(false);
                }
            }
        };

        RuleBasedConditionEvaluator evaluator = new RuleBasedConditionEvaluator(mockEvaluator, conditions);

        assertTrue(evaluator.test(0));
        assertFalse(evaluator.test(1));
    }

    @Test
    void testHandlesEmptyValue() {
        Condition cond = Condition.builder().fn(TestHelpers.isSet("param")).build();
        Condition[] conditions = {cond};

        RuleEvaluator mockEvaluator = new RuleEvaluator() {
            @Override
            public Value evaluateCondition(Condition condition) {
                return Value.emptyValue();
            }
        };

        RuleBasedConditionEvaluator evaluator = new RuleBasedConditionEvaluator(mockEvaluator, conditions);
        assertFalse(evaluator.test(0));
    }

    @Test
    void testHandlesNonBooleanTruthyValue() {
        Condition cond = Condition.builder().fn(TestHelpers.parseUrl("https://example.com")).build();
        Condition[] conditions = {cond};

        RuleEvaluator mockEvaluator = new RuleEvaluator() {
            @Override
            public Value evaluateCondition(Condition condition) {
                return Value.stringValue("some-string");
            }
        };

        RuleBasedConditionEvaluator evaluator = new RuleBasedConditionEvaluator(mockEvaluator, conditions);
        assertTrue(evaluator.test(0));
    }
}
