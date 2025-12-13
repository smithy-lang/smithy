/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.BooleanEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.logic.ConditionCostModel;
import software.amazon.smithy.rulesengine.logic.TestHelpers;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;
import software.amazon.smithy.rulesengine.traits.EndpointBddTrait;

class CostOptimizationTest {

    @Test
    void builderRequiresCfg() {
        assertThrows(IllegalStateException.class, () -> CostOptimization.builder().build());
    }

    @Test
    void basicOptimizationPreservesSemantics() {
        EndpointRuleSet ruleSet = createSimpleRuleSet();
        Cfg cfg = Cfg.from(ruleSet);
        EndpointBddTrait originalTrait = EndpointBddTrait.from(cfg);

        CostOptimization optimizer = CostOptimization.builder().cfg(cfg).build();
        EndpointBddTrait optimizedTrait = optimizer.apply(originalTrait);

        // Must preserve structure
        assertEquals(originalTrait.getConditions().size(), optimizedTrait.getConditions().size());
        assertEquals(originalTrait.getResults().size(), optimizedTrait.getResults().size());
        assertEquals(originalTrait.getBdd().getConditionCount(), optimizedTrait.getBdd().getConditionCount());
        assertEquals(originalTrait.getBdd().getResultCount(), optimizedTrait.getBdd().getResultCount());
    }

    @Test
    void optimizationWithCustomCostModel() {
        EndpointRuleSet ruleSet = createSimpleRuleSet();
        Cfg cfg = Cfg.from(ruleSet);
        EndpointBddTrait originalTrait = EndpointBddTrait.from(cfg);

        CostOptimization optimizer = CostOptimization.builder()
                .cfg(cfg)
                .costModel(ConditionCostModel.createUniform())
                .build();
        EndpointBddTrait optimizedTrait = optimizer.apply(originalTrait);

        assertNotNull(optimizedTrait);
        assertEquals(originalTrait.getConditions().size(), optimizedTrait.getConditions().size());
    }

    @Test
    void optimizationWithCustomProbabilities() {
        EndpointRuleSet ruleSet = createSimpleRuleSet();
        Cfg cfg = Cfg.from(ruleSet);
        EndpointBddTrait originalTrait = EndpointBddTrait.from(cfg);

        CostOptimization optimizer = CostOptimization.builder()
                .cfg(cfg)
                .trueProbability(c -> 0.9) // 90% true probability for all
                .build();
        EndpointBddTrait optimizedTrait = optimizer.apply(originalTrait);

        assertNotNull(optimizedTrait);
        assertEquals(originalTrait.getConditions().size(), optimizedTrait.getConditions().size());
    }

    @Test
    void optimizationRespectsNodeBudget() {
        EndpointRuleSet ruleSet = createSimpleRuleSet();
        Cfg cfg = Cfg.from(ruleSet);
        EndpointBddTrait originalTrait = EndpointBddTrait.from(cfg);
        int originalNodes = originalTrait.getBdd().getNodeCount();

        // Allow 10% growth
        CostOptimization optimizer = CostOptimization.builder()
                .cfg(cfg)
                .maxAllowedGrowth(0.1)
                .build();
        EndpointBddTrait optimizedTrait = optimizer.apply(originalTrait);

        // Node count should be within budget (original + 10%)
        int maxAllowed = originalNodes + (int) (originalNodes * 0.1);
        assertTrue(optimizedTrait.getBdd().getNodeCount() <= maxAllowed + 1, // +1 for rounding
                "Node count should respect budget");
    }

    @Test
    void optimizationWithZeroGrowthAllowed() {
        EndpointRuleSet ruleSet = createSimpleRuleSet();
        Cfg cfg = Cfg.from(ruleSet);
        EndpointBddTrait originalTrait = EndpointBddTrait.from(cfg);
        int originalNodes = originalTrait.getBdd().getNodeCount();

        // Allow no growth
        CostOptimization optimizer = CostOptimization.builder()
                .cfg(cfg)
                .maxAllowedGrowth(0.0)
                .build();
        EndpointBddTrait optimizedTrait = optimizer.apply(originalTrait);

        // Node count should not exceed original
        assertTrue(optimizedTrait.getBdd().getNodeCount() <= originalNodes);
    }

    @Test
    void optimizationWithMaxRoundsLimit() {
        EndpointRuleSet ruleSet = createSimpleRuleSet();
        Cfg cfg = Cfg.from(ruleSet);
        EndpointBddTrait originalTrait = EndpointBddTrait.from(cfg);

        // Single round
        CostOptimization optimizer = CostOptimization.builder()
                .cfg(cfg)
                .maxRounds(1)
                .build();
        EndpointBddTrait optimizedTrait = optimizer.apply(originalTrait);

        assertNotNull(optimizedTrait);
        assertEquals(originalTrait.getConditions().size(), optimizedTrait.getConditions().size());
    }

    @Test
    void optimizationWithLowTopK() {
        EndpointRuleSet ruleSet = createSimpleRuleSet();
        Cfg cfg = Cfg.from(ruleSet);
        EndpointBddTrait originalTrait = EndpointBddTrait.from(cfg);

        // Only consider top 1 hot position
        CostOptimization optimizer = CostOptimization.builder()
                .cfg(cfg)
                .topK(1)
                .build();
        EndpointBddTrait optimizedTrait = optimizer.apply(originalTrait);

        assertNotNull(optimizedTrait);
    }

    @Test
    void optimizationPreservesDependencies() {
        // Create ruleset where condition B depends on result from A
        // The dependency constraint system ensures correct ordering
        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("Input").type(ParameterType.STRING).build())
                .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                .build();

        Rule rule = EndpointRule.builder()
                .conditions(
                        Condition.builder()
                                .fn(TestHelpers.isSet("Input"))
                                .result(Identifier.of("hasInput"))
                                .build(),
                        Condition.builder()
                                .fn(BooleanEquals.ofExpressions(
                                        Expression.getReference(Identifier.of("hasInput")),
                                        Literal.of(true)))
                                .build(),
                        Condition.builder().fn(TestHelpers.isSet("Region")).build())
                .endpoint(TestHelpers.endpoint("https://example.com"));

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule)
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        EndpointBddTrait originalTrait = EndpointBddTrait.from(cfg);

        CostOptimization optimizer = CostOptimization.builder().cfg(cfg).build();
        EndpointBddTrait optimizedTrait = optimizer.apply(originalTrait);

        // If dependencies were violated, the BDD compilation would fail or produce
        // incorrect results. The fact that optimization completes successfully proves
        // dependencies are preserved.
        assertNotNull(optimizedTrait);
        assertEquals(originalTrait.getConditions().size(), optimizedTrait.getConditions().size());
    }

    @Test
    void optimizationWithMultipleRules() {
        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder()
                        .name("UseFips")
                        .type(ParameterType.BOOLEAN)
                        .required(true)
                        .defaultValue(Value.booleanValue(false))
                        .build())
                .addParameter(Parameter.builder()
                        .name("UseDualStack")
                        .type(ParameterType.BOOLEAN)
                        .required(true)
                        .defaultValue(Value.booleanValue(false))
                        .build())
                .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                .build();

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(EndpointRule.builder()
                        .conditions(
                                Condition.builder().fn(TestHelpers.booleanEquals("UseFips", true)).build(),
                                Condition.builder().fn(TestHelpers.isSet("Region")).build())
                        .endpoint(TestHelpers.endpoint("https://fips.example.com")))
                .addRule(EndpointRule.builder()
                        .conditions(
                                Condition.builder().fn(TestHelpers.booleanEquals("UseDualStack", true)).build(),
                                Condition.builder().fn(TestHelpers.isSet("Region")).build())
                        .endpoint(TestHelpers.endpoint("https://dualstack.example.com")))
                .addRule(EndpointRule.builder()
                        .conditions(Condition.builder().fn(TestHelpers.isSet("Region")).build())
                        .endpoint(TestHelpers.endpoint("https://example.com")))
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        EndpointBddTrait originalTrait = EndpointBddTrait.from(cfg);

        CostOptimization optimizer = CostOptimization.builder()
                .cfg(cfg)
                .trueProbability(c -> {
                    // Simulate AWS-like priors: boolean flags are usually false
                    String s = c.toString();
                    if (s.contains("booleanEquals") && s.contains("true")) {
                        return 0.05;
                    }
                    return 0.5;
                })
                .build();
        EndpointBddTrait optimizedTrait = optimizer.apply(originalTrait);

        assertNotNull(optimizedTrait);
        assertEquals(originalTrait.getResults().size(), optimizedTrait.getResults().size());
    }

    private EndpointRuleSet createSimpleRuleSet() {
        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("A").type(ParameterType.STRING).build())
                .addParameter(Parameter.builder().name("B").type(ParameterType.STRING).build())
                .addParameter(Parameter.builder().name("C").type(ParameterType.STRING).build())
                .build();

        Rule rule = EndpointRule.builder()
                .conditions(
                        Condition.builder().fn(TestHelpers.isSet("A")).build(),
                        Condition.builder().fn(TestHelpers.isSet("B")).build(),
                        Condition.builder().fn(TestHelpers.isSet("C")).build())
                .endpoint(TestHelpers.endpoint("https://example.com"));

        return EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule)
                .build();
    }
}
