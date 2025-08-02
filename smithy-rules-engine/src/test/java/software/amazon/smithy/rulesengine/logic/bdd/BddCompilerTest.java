/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
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

class BddCompilerTest {

    // Common parameters used across tests
    private static final Parameter REGION_PARAM = Parameter.builder()
            .name("Region")
            .type(ParameterType.STRING)
            .build();

    private static final Parameter BUCKET_PARAM = Parameter.builder()
            .name("Bucket")
            .type(ParameterType.STRING)
            .build();

    private static final Parameter A_PARAM = Parameter.builder()
            .name("A")
            .type(ParameterType.STRING)
            .build();

    private static final Parameter B_PARAM = Parameter.builder()
            .name("B")
            .type(ParameterType.STRING)
            .build();

    @Test
    void testCompileSimpleEndpointRule() {
        // Single rule with one condition
        Rule rule = EndpointRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("Region")).build())
                .endpoint(TestHelpers.endpoint("https://example.com"));

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder().addParameter(REGION_PARAM).build())
                .addRule(rule)
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        BddCompiler compiler = new BddCompiler(cfg, ConditionOrderingStrategy.defaultOrdering(), new BddBuilder());

        Bdd bdd = compiler.compile();

        assertNotNull(bdd);
        assertEquals(1, bdd.getConditionCount());
        // Results include: endpoint when condition true, no match when false
        assertTrue(bdd.getResultCount() >= 2);
        assertTrue(bdd.getRootRef() > 0);
    }

    @Test
    void testCompileErrorRule() {
        // Error rule instead of endpoint
        Rule rule = ErrorRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("Bucket")).build())
                .error("Bucket is required");

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder().addParameter(BUCKET_PARAM).build())
                .addRule(rule)
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        BddCompiler compiler = new BddCompiler(cfg, ConditionOrderingStrategy.defaultOrdering(), new BddBuilder());

        Bdd bdd = compiler.compile();

        assertEquals(1, bdd.getConditionCount());
        // Similar to endpoint rule
        assertTrue(bdd.getResultCount() >= 2);
    }

    @Test
    void testCompileTreeRule() {
        // Nested tree rule
        Rule nestedRule = EndpointRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("Bucket")).build())
                .endpoint(TestHelpers.endpoint("https://bucket.example.com"));
        Rule treeRule = TreeRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("Region")).build())
                .treeRule(nestedRule);
        Parameters params = Parameters.builder().addParameter(REGION_PARAM).addParameter(BUCKET_PARAM).build();
        EndpointRuleSet ruleSet = EndpointRuleSet.builder().parameters(params).addRule(treeRule).build();

        Cfg cfg = Cfg.from(ruleSet);
        BddCompiler compiler = new BddCompiler(cfg, ConditionOrderingStrategy.defaultOrdering(), new BddBuilder());

        Bdd bdd = compiler.compile();

        assertEquals(2, bdd.getConditionCount());
        assertTrue(bdd.getNodeCount() > 2); // Should have multiple nodes
    }

    @Test
    void testCompileWithCustomOrdering() {
        // Multiple conditions to test ordering
        Rule rule = EndpointRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("A")).build(),
                        Condition.builder().fn(TestHelpers.isSet("B")).build())
                .endpoint(TestHelpers.endpoint("https://example.com"));
        Parameters params = Parameters.builder().addParameter(A_PARAM).addParameter(B_PARAM).build();
        EndpointRuleSet ruleSet = EndpointRuleSet.builder().parameters(params).addRule(rule).build();

        Cfg cfg = Cfg.from(ruleSet);

        // Get the actual conditions from the CFG after SSA transform
        Condition[] cfgConditions = cfg.getConditions();

        // Find the conditions that correspond to A and B
        Condition condA = null;
        Condition condB = null;
        for (Condition c : cfgConditions) {
            String condStr = c.toString();
            if (condStr.contains("isSet(A)")) {
                condA = c;
            } else if (condStr.contains("isSet(B)")) {
                condB = c;
            }
        }

        assertNotNull(condA, "Could not find condition for A");
        assertNotNull(condB, "Could not find condition for B");

        // Use fixed ordering (B before A)
        ConditionOrderingStrategy customOrdering = ConditionOrderingStrategy.fixed(Arrays.asList(condB, condA));

        BddCompiler compiler = new BddCompiler(cfg, customOrdering, new BddBuilder());
        Bdd bdd = compiler.compile();

        // Verify ordering was applied by checking the compiled BDD
        // Since we don't have access to conditions from Bdd anymore, we just verify compilation succeeded
        assertEquals(2, bdd.getConditionCount());
        assertNotNull(bdd);
    }

    @Test
    void testCompileEmptyRuleSet() {
        // No rules
        EndpointRuleSet ruleSet = EndpointRuleSet.builder().parameters(Parameters.builder().build()).build();

        Cfg cfg = Cfg.from(ruleSet);
        BddCompiler compiler = new BddCompiler(cfg, ConditionOrderingStrategy.defaultOrdering(), new BddBuilder());
        Bdd bdd = compiler.compile();

        assertEquals(0, bdd.getConditionCount());
        // Even with no rules, there's still a result (no match)
        assertTrue(bdd.getResultCount() > 0);
        // Should have at least terminal node
        assertTrue(bdd.getNodeCount() > 0);
    }

    @Test
    void testCompileSameResultMultiplePaths() {
        // Two rules leading to same endpoint
        Rule rule1 = EndpointRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("Region")).build())
                .endpoint(TestHelpers.endpoint("https://example.com"));

        Rule rule2 = EndpointRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("Bucket")).build())
                .endpoint(TestHelpers.endpoint("https://example.com"));

        Parameters params = Parameters.builder()
                .addParameter(REGION_PARAM)
                .addParameter(BUCKET_PARAM)
                .build();

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule1)
                .addRule(rule2)
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        BddCompiler compiler = new BddCompiler(cfg, ConditionOrderingStrategy.defaultOrdering(), new BddBuilder());

        Bdd bdd = compiler.compile();

        // The BDD compiler might create separate result nodes even for same endpoint
        // depending on how the CFG is structured
        assertEquals(3, bdd.getResultCount());
    }
}
