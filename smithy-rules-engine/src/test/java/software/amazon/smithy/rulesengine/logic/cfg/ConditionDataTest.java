/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;
import software.amazon.smithy.rulesengine.logic.ConditionInfo;
import software.amazon.smithy.rulesengine.logic.TestHelpers;

class ConditionDataTest {

    @Test
    void extractsConditionsFromSimpleCfg() {
        // Build a simple ruleset with two conditions
        Condition cond1 = Condition.builder()
                .fn(TestHelpers.isSet("param1"))
                .build();

        // For stringEquals, we need to ensure param2 is set first
        Rule rule = TreeRule.builder()
                .condition(cond1)
                .treeRule(
                        TreeRule.builder()
                                .condition(Condition.builder().fn(TestHelpers.isSet("param2")).build())
                                .treeRule(
                                        EndpointRule.builder()
                                                .condition(Condition.builder()
                                                        .fn(TestHelpers.stringEquals("param2", "value"))
                                                        .build())
                                                .endpoint(TestHelpers.endpoint("https://example.com"))));

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("param1").type(ParameterType.STRING).build())
                .addParameter(Parameter.builder().name("param2").type(ParameterType.STRING).build())
                .build();

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .addRule(rule)
                .parameters(params)
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        ConditionData data = ConditionData.from(cfg);

        // Verify condition extraction
        Condition[] conditions = data.getConditions();
        assertEquals(3, conditions.length); // isSet(param1), isSet(param2), stringEquals

        // Verify condition infos
        Map<Condition, ConditionInfo> infos = data.getConditionInfos();
        assertEquals(3, infos.size());
    }

    @Test
    void deduplicatesIdenticalConditions() {
        // Create identical conditions used in different rules
        Condition cond = Condition.builder()
                .fn(TestHelpers.isSet("param"))
                .build();

        Rule rule1 = EndpointRule.builder().conditions(cond).endpoint(TestHelpers.endpoint("https://endpoint1.com"));
        Rule rule2 = EndpointRule.builder().conditions(cond).endpoint(TestHelpers.endpoint("https://endpoint2.com"));

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("param").type(ParameterType.STRING).build())
                .build();

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule1)
                .addRule(rule2)
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        ConditionData data = ConditionData.from(cfg);

        // Should only have one condition despite being used twice
        assertEquals(1, data.getConditions().length);
        assertEquals(cond, data.getConditions()[0]);
    }

    @Test
    void handlesNestedTreeRules() {
        Condition cond1 = Condition.builder().fn(TestHelpers.isSet("param1")).build();
        Condition cond2 = Condition.builder().fn(TestHelpers.isSet("param2")).build();
        Condition cond3 = Condition.builder().fn(TestHelpers.isSet("param3")).build();

        Rule innerRule = TreeRule.builder()
                .conditions(cond2)
                .treeRule(EndpointRule.builder()
                        .condition(cond3)
                        .endpoint(TestHelpers.endpoint("https://example.com")));

        Rule outerRule = TreeRule.builder().condition(cond1).treeRule(innerRule);

        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("param1").type(ParameterType.STRING).build())
                .addParameter(Parameter.builder().name("param2").type(ParameterType.STRING).build())
                .addParameter(Parameter.builder().name("param3").type(ParameterType.STRING).build())
                .build();

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .addRule(outerRule)
                .parameters(params)
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        ConditionData data = ConditionData.from(cfg);

        // Should extract all three conditions
        assertEquals(3, data.getConditions().length);
        assertEquals(3, data.getConditionInfos().size());
    }

    @Test
    void handlesCfgWithOnlyResults() {
        // Rule with no conditions, just a result
        Rule rule = EndpointRule.builder().endpoint(TestHelpers.endpoint("https://default.com"));
        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder().build())
                .addRule(rule)
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        ConditionData data = ConditionData.from(cfg);

        // Should have no conditions
        assertEquals(0, data.getConditions().length);
        assertTrue(data.getConditionInfos().isEmpty());
    }

    @Test
    void cachesResultOnCfg() {
        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("param").type(ParameterType.STRING).build())
                .build();

        EndpointRule rule = EndpointRule.builder()
                .condition(Condition.builder().fn(TestHelpers.isSet("param")).build())
                .endpoint(TestHelpers.endpoint("https://example.com"));
        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule)
                .build();
        Cfg cfg = Cfg.from(ruleSet);

        // First call should create the data
        ConditionData data1 = cfg.getConditionData();
        assertNotNull(data1);

        // Second call should return the same instance
        ConditionData data2 = cfg.getConditionData();
        assertSame(data1, data2);
    }
}
