/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.logic.ConditionEvaluator;
import software.amazon.smithy.rulesengine.logic.TestHelpers;
import software.amazon.smithy.utils.ListUtils;

class BddEvaluatorTest {

    private static final Parameters EMPTY = Parameters.builder().build();

    @Test
    void testEvaluateTerminalTrue() {
        // BDD with just TRUE terminal
        int[][] nodes = new int[][] {{-1, 1, -1}};
        Bdd bdd = new Bdd(EMPTY, ListUtils.of(), ListUtils.of(), nodes, 1);

        BddEvaluator evaluator = BddEvaluator.from(bdd);
        int result = evaluator.evaluate(idx -> true);

        assertEquals(-1, result); // TRUE terminal returns -1 (TRUE isn't valid in our MTBDD)
    }

    @Test
    void testEvaluateTerminalFalse() {
        // BDD with just FALSE terminal
        int[][] nodes = new int[][] {{-1, 1, -1}};
        Bdd bdd = new Bdd(EMPTY, ListUtils.of(), ListUtils.of(), nodes, -1);

        BddEvaluator evaluator = BddEvaluator.from(bdd);
        int result = evaluator.evaluate(idx -> true);

        assertEquals(-1, result); // FALSE terminal returns -1 (same as TRUE; FALSE isn't valid in our MTBDD).
    }

    @Test
    void testEvaluateSingleConditionTrue() {
        Condition cond = Condition.builder().fn(TestHelpers.isSet("param")).build();
        Rule rule = EndpointRule.builder().endpoint(TestHelpers.endpoint("https://example.com"));

        // BDD: if condition then result1 else no-match
        // With new encoding: result references are encoded as RESULT_OFFSET + resultIndex
        int result1Ref = Bdd.RESULT_OFFSET + 1;

        int[][] nodes = new int[][] {
                {-1, 1, -1}, // 0: terminal
                {0, result1Ref, -1} // 1: condition node (high=result1, low=FALSE)
        };
        Bdd bdd = new Bdd(EMPTY, ListUtils.of(cond), ListUtils.of(null, rule), nodes, 2);

        BddEvaluator evaluator = BddEvaluator.from(bdd);

        // When condition is true, should return result 1
        assertEquals(1, evaluator.evaluate(idx -> true));

        // When condition is false, should return -1 (no match)
        assertEquals(-1, evaluator.evaluate(idx -> false));
    }

    @Test
    void testEvaluateComplementedNode() {
        Condition cond1 = Condition.builder().fn(TestHelpers.isSet("param1")).build();
        Condition cond2 = Condition.builder().fn(TestHelpers.isSet("param2")).build();
        Rule rule = EndpointRule.builder().endpoint(TestHelpers.endpoint("https://example.com"));

        // BDD with a complemented reference to an internal node
        // We want: if cond1 then NOT(cond2) else false
        // Which means: if cond1 && !cond2 then result1 else no-match
        int result1Ref = Bdd.RESULT_OFFSET + 1;

        int[][] nodes = new int[][] {
                {-1, 1, -1}, // 0: terminal
                {0, -3, -1}, // 1: cond1 node (high=-3 (complement of ref 3 = node 2), low=FALSE)
                {1, -1, result1Ref} // 2: cond2 node (high=FALSE, low=result1)
        };
        // Root is 2 (reference to node at index 1)
        Bdd bdd = new Bdd(EMPTY, ListUtils.of(cond1, cond2), ListUtils.of(null, rule), nodes, 2);

        BddEvaluator evaluator = BddEvaluator.from(bdd);

        // When cond1=true, we follow high branch to -3 (complement of node 2)
        // The complement flips the node's branch selection
        // If cond2=true, with complement we take the "false" branch (which is low) -> result1
        // If cond2=false, with complement we take the "true" branch (which is high) -> FALSE
        ConditionEvaluator bothTrue = idx -> true;
        assertEquals(1, evaluator.evaluate(bothTrue)); // cond1=true, cond2=true -> result1

        ConditionEvaluator firstTrueSecondFalse = idx -> idx == 0;
        assertEquals(-1, evaluator.evaluate(firstTrueSecondFalse)); // cond1=true, cond2=false -> FALSE

        // When cond1=false, we follow low branch to FALSE
        ConditionEvaluator firstFalse = idx -> false;
        assertEquals(-1, evaluator.evaluate(firstFalse)); // cond1=false -> FALSE
    }

    @Test
    void testEvaluateMultipleConditions() {
        Condition cond1 = Condition.builder().fn(TestHelpers.isSet("param1")).build();
        Condition cond2 = Condition.builder().fn(TestHelpers.isSet("param2")).build();
        Rule rule1 = EndpointRule.builder().endpoint(TestHelpers.endpoint("https://a.com"));
        Rule rule2 = ErrorRule.builder().error("hi");

        // BDD: if cond1 then (if cond2 then result1 else result2) else no-match
        // Result references encoded with RESULT_OFFSET
        int result1Ref = Bdd.RESULT_OFFSET + 1;
        int result2Ref = Bdd.RESULT_OFFSET + 2;

        int[][] nodes = new int[][] {
                {-1, 1, -1}, // 0: terminal
                {0, 3, -1}, // 1: cond1 node (high=cond2 node, low=FALSE)
                {1, result1Ref, result2Ref} // 2: cond2 node (high=result1, low=result2)
        };
        Bdd bdd = new Bdd(EMPTY, ListUtils.of(cond1, cond2), ListUtils.of(null, rule1, rule2), nodes, 2);

        BddEvaluator evaluator = BddEvaluator.from(bdd);
        ConditionEvaluator condEval = idx -> idx == 0; // only first condition is true

        int result = evaluator.evaluate(condEval);
        assertEquals(2, result); // Should get result2 since cond2 is false
    }

    @Test
    void testEvaluateNoMatchResult() {
        Condition cond = Condition.builder().fn(TestHelpers.isSet("param")).build();

        // BDD with explicit no-match result (index 0)
        // Result0 reference encoded with RESULT_OFFSET
        int result0Ref = Bdd.RESULT_OFFSET;

        int[][] nodes = new int[][] {
                {-1, 1, -1}, // 0: terminal
                {0, -1, result0Ref} // 1: condition node
        };
        Bdd bdd = new Bdd(EMPTY, ListUtils.of(cond), ListUtils.of((Rule) null), nodes, 2);

        BddEvaluator evaluator = BddEvaluator.from(bdd);
        int result = evaluator.evaluate(idx -> false);

        assertEquals(-1, result); // Result index 0 is treated as no-match
    }

    @Test
    void testEvaluateWithLargeResultIndex() {
        Condition cond = Condition.builder().fn(TestHelpers.isSet("param")).build();
        Rule rule = EndpointRule.builder().endpoint(TestHelpers.endpoint("https://example.com"));

        // Test with a larger result index to ensure offset works correctly
        int result999Ref = Bdd.RESULT_OFFSET + 999;

        int[][] nodes = new int[][] {
                {-1, 1, -1}, // 0: terminal
                {0, result999Ref, -1} // 1: condition node
        };

        // Create a results list with 1000 entries (0-999)
        Rule[] results = new Rule[1000];
        results[999] = rule;

        Bdd bdd = new Bdd(EMPTY, ListUtils.of(cond), ListUtils.of(results), nodes, 2);

        BddEvaluator evaluator = BddEvaluator.from(bdd);

        // When condition is true, should return result 999
        assertEquals(999, evaluator.evaluate(idx -> true));
    }

    @Test
    void testEvaluateComplexBddWithMixedReferences() {
        Condition cond1 = Condition.builder().fn(TestHelpers.isSet("param1")).build();
        Condition cond2 = Condition.builder().fn(TestHelpers.isSet("param2")).build();
        Condition cond3 = Condition.builder().fn(TestHelpers.isSet("param3")).build();
        Rule rule1 = EndpointRule.builder().endpoint(TestHelpers.endpoint("https://a.com"));
        Rule rule2 = ErrorRule.builder().error("error");

        // Complex BDD with multiple conditions, complement edges, and results
        int result1Ref = Bdd.RESULT_OFFSET + 1;
        int result2Ref = Bdd.RESULT_OFFSET + 2;

        int[][] nodes = new int[][] {
                {-1, 1, -1}, // 0: terminal
                {0, 3, 4}, // 1: cond1 node
                {1, result1Ref, -1}, // 2: cond2 node
                {2, result2Ref, -5} // 3: cond3 node (low has complement ref)
        };

        Bdd bdd = new Bdd(EMPTY,
                ListUtils.of(cond1, cond2, cond3),
                ListUtils.of(null, rule1, rule2),
                nodes,
                2);

        BddEvaluator evaluator = BddEvaluator.from(bdd);

        // Test various paths through the BDD
        ConditionEvaluator allTrue = idx -> true;
        assertEquals(1, evaluator.evaluate(allTrue)); // cond1=T -> cond2=T -> result1

        ConditionEvaluator firstTrueOnly = idx -> idx == 0;
        assertEquals(-1, evaluator.evaluate(firstTrueOnly)); // cond1=T -> cond2=F -> FALSE

        ConditionEvaluator firstFalseThirdTrue = idx -> idx == 2;
        assertEquals(2, evaluator.evaluate(firstFalseThirdTrue)); // cond1=F -> cond3=T -> result2
    }
}
