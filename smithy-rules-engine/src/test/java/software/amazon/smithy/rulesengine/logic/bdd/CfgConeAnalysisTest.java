/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;
import software.amazon.smithy.rulesengine.logic.TestHelpers;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;
import software.amazon.smithy.utils.ListUtils;

class CfgConeAnalysisTest {

    @Test
    void testSingleConditionSingleResult() {
        // Simple rule: if Region is set, return endpoint
        Rule rule = EndpointRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("Region")).build())
                .endpoint(TestHelpers.endpoint("https://example.com"));

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder()
                        .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                        .build())
                .addRule(rule)
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        Condition[] conditions = cfg.getConditions();
        Map<Condition, Integer> conditionToIndex = new HashMap<>();
        for (int i = 0; i < conditions.length; i++) {
            conditionToIndex.put(conditions[i], i);
        }

        CfgConeAnalysis analysis = new CfgConeAnalysis(cfg, conditions, conditionToIndex);

        // The single condition should have dominator depth 0 (at root)
        assertEquals(0, analysis.dominatorDepth(0));
        // Should reach 2 result nodes (the endpoint and the terminal/no-match)
        assertEquals(2, analysis.coneSize(0));
    }

    @Test
    void testChainedConditions() {
        // Rule with two conditions in sequence (AND logic)
        Rule rule = EndpointRule.builder()
                .conditions(
                        Condition.builder().fn(TestHelpers.isSet("Region")).build(),
                        Condition.builder().fn(TestHelpers.isSet("Bucket")).build())
                .endpoint(TestHelpers.endpoint("https://example.com"));

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                .addParameter(Parameter.builder().name("Bucket").type(ParameterType.STRING).build())
                .build();

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule)
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        Condition[] conditions = cfg.getConditions();
        Map<Condition, Integer> conditionToIndex = new HashMap<>();
        for (int i = 0; i < conditions.length; i++) {
            conditionToIndex.put(conditions[i], i);
        }

        CfgConeAnalysis analysis = new CfgConeAnalysis(cfg, conditions, conditionToIndex);

        // First condition should be at depth 0
        assertEquals(0, analysis.dominatorDepth(0));
        // Second condition should be at depth 1 (one edge from root)
        assertEquals(1, analysis.dominatorDepth(1));

        // Both conditions lead to the same result
        assertTrue(analysis.coneSize(0) >= 1);
        assertTrue(analysis.coneSize(1) >= 1);
    }

    @Test
    void testMultipleBranches() {
        // Two separate rules leading to different endpoints
        Rule rule1 = EndpointRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("Region")).build())
                .endpoint(TestHelpers.endpoint("https://regional.com"));

        Rule rule2 = EndpointRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("Bucket")).build())
                .endpoint(TestHelpers.endpoint("https://bucket.com"));

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                .addParameter(Parameter.builder().name("Bucket").type(ParameterType.STRING).build())
                .build();

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .rules(ListUtils.of(rule1, rule2))
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        Condition[] conditions = cfg.getConditions();
        Map<Condition, Integer> conditionToIndex = new HashMap<>();
        for (int i = 0; i < conditions.length; i++) {
            conditionToIndex.put(conditions[i], i);
        }

        CfgConeAnalysis analysis = new CfgConeAnalysis(cfg, conditions, conditionToIndex);

        // Both conditions should be at the root level (depth 0 or 1)
        assertTrue(analysis.dominatorDepth(0) <= 1);
        assertTrue(analysis.dominatorDepth(1) <= 1);

        // Each condition reaches at least one result
        assertTrue(analysis.coneSize(0) >= 1);
        assertTrue(analysis.coneSize(1) >= 1);
    }

    @Test
    void testNestedTreeRule() {
        // Tree rule with nested structure
        Rule innerRule = EndpointRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("Bucket")).build())
                .endpoint(TestHelpers.endpoint("https://bucket.com"));

        Rule treeRule = TreeRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("Region")).build())
                .treeRule(innerRule);

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                .addParameter(Parameter.builder().name("Bucket").type(ParameterType.STRING).build())
                .build();

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(treeRule)
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        Condition[] conditions = cfg.getConditions();
        Map<Condition, Integer> conditionToIndex = new HashMap<>();
        for (int i = 0; i < conditions.length; i++) {
            conditionToIndex.put(conditions[i], i);
        }

        CfgConeAnalysis analysis = new CfgConeAnalysis(cfg, conditions, conditionToIndex);

        // Region condition should be at root (depth 0)
        int regionIdx = -1;
        int bucketIdx = -1;
        for (int i = 0; i < conditions.length; i++) {
            if (conditions[i].toString().contains("Region")) {
                regionIdx = i;
            } else if (conditions[i].toString().contains("Bucket")) {
                bucketIdx = i;
            }
        }

        if (regionIdx >= 0) {
            assertEquals(0, analysis.dominatorDepth(regionIdx));
        }

        // Bucket condition should be deeper (at least depth 1)
        if (bucketIdx >= 0) {
            assertTrue(analysis.dominatorDepth(bucketIdx) >= 1);
        }
    }

    @Test
    void testErrorRule() {
        // Rule that returns an error instead of endpoint
        Rule rule = ErrorRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("InvalidParam")).build())
                .error("Invalid parameter provided");

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder()
                        .addParameter(Parameter.builder().name("InvalidParam").type(ParameterType.STRING).build())
                        .build())
                .addRule(rule)
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        Condition[] conditions = cfg.getConditions();
        Map<Condition, Integer> conditionToIndex = new HashMap<>();
        for (int i = 0; i < conditions.length; i++) {
            conditionToIndex.put(conditions[i], i);
        }

        CfgConeAnalysis analysis = new CfgConeAnalysis(cfg, conditions, conditionToIndex);

        // The condition should be at root
        assertEquals(0, analysis.dominatorDepth(0));
        // Should reach 2 results (the error and terminal/no-match)
        assertEquals(2, analysis.coneSize(0));
    }

    @Test
    void testEmptyCfg() {
        // Empty ruleset
        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder().build())
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        Condition[] conditions = cfg.getConditions();

        // Should have no conditions
        assertEquals(0, conditions.length);

        // Analysis should handle empty CFG gracefully
        Map<Condition, Integer> conditionToIndex = new HashMap<>();
        CfgConeAnalysis analysis = new CfgConeAnalysis(cfg, conditions, conditionToIndex);

        // No assertions needed - just verify it doesn't throw
    }
}
