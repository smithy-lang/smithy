/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.logic.ConditionCostModel;
import software.amazon.smithy.rulesengine.logic.TestHelpers;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;

class BddCostEstimatorTest {

    @Test
    void terminalBddHasZeroCost() {
        // BDD with terminal root (TRUE)
        Bdd bdd = new Bdd(1, 0, 1, 1, consumer -> {
            consumer.accept(-1, 1, -1); // terminal node
        });

        List<Condition> ordering = List.of();
        BddCostEstimator estimator = new BddCostEstimator(ordering, ConditionCostModel.createUniform(), null);

        assertEquals(0.0, estimator.expectedCost(bdd, ordering));
    }

    @Test
    void singleConditionBddCost() {
        // Create a simple ruleset with one condition
        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder()
                        .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                        .build())
                .addRule(EndpointRule.builder()
                        .conditions(Condition.builder().fn(TestHelpers.isSet("Region")).build())
                        .endpoint(TestHelpers.endpoint("https://example.com")))
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        Bdd bdd = new BddCompiler(cfg, OrderingStrategy.initialOrdering(cfg), new BddBuilder()).compile();
        List<Condition> conditions = Arrays.asList(cfg.getConditions());

        // With uniform cost (1) and uniform probability (0.5), the root condition is always evaluated
        BddCostEstimator estimator = new BddCostEstimator(conditions, ConditionCostModel.createUniform(), null);
        double cost = estimator.expectedCost(bdd, conditions);

        // Root condition is always reached (probability 1.0), cost is 1
        assertEquals(1.0, cost, 0.001);
    }

    @Test
    void costReflectsReachProbability() {
        // Create ruleset with two conditions: if A then (if B then endpoint)
        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder()
                        .addParameter(Parameter.builder()
                                .name("A")
                                .type(ParameterType.BOOLEAN)
                                .required(true)
                                .defaultValue(Value.booleanValue(false))
                                .build())
                        .addParameter(Parameter.builder()
                                .name("B")
                                .type(ParameterType.BOOLEAN)
                                .required(true)
                                .defaultValue(Value.booleanValue(false))
                                .build())
                        .build())
                .addRule(EndpointRule.builder()
                        .conditions(
                                Condition.builder().fn(TestHelpers.booleanEquals("A", true)).build(),
                                Condition.builder().fn(TestHelpers.booleanEquals("B", true)).build())
                        .endpoint(TestHelpers.endpoint("https://example.com")))
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        Bdd bdd = new BddCompiler(cfg, OrderingStrategy.initialOrdering(cfg), new BddBuilder()).compile();
        List<Condition> conditions = Arrays.asList(cfg.getConditions());

        // With uniform probability 0.5:
        // - Condition A is always evaluated (reach prob = 1.0)
        // - Condition B is only evaluated if A is true (reach prob = 0.5)
        // Expected cost = 1.0 * 1 + 0.5 * 1 = 1.5
        BddCostEstimator estimator = new BddCostEstimator(conditions, ConditionCostModel.createUniform(), null);
        double cost = estimator.expectedCost(bdd, conditions);

        assertEquals(1.5, cost, 0.001);
    }

    @Test
    void customProbabilityAffectsCost() {
        // Create ruleset with two sequential conditions
        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder()
                        .addParameter(Parameter.builder()
                                .name("A")
                                .type(ParameterType.BOOLEAN)
                                .required(true)
                                .defaultValue(Value.booleanValue(false))
                                .build())
                        .addParameter(Parameter.builder()
                                .name("B")
                                .type(ParameterType.BOOLEAN)
                                .required(true)
                                .defaultValue(Value.booleanValue(false))
                                .build())
                        .build())
                .addRule(EndpointRule.builder()
                        .conditions(
                                Condition.builder().fn(TestHelpers.booleanEquals("A", true)).build(),
                                Condition.builder().fn(TestHelpers.booleanEquals("B", true)).build())
                        .endpoint(TestHelpers.endpoint("https://example.com")))
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        Bdd bdd = new BddCompiler(cfg, OrderingStrategy.initialOrdering(cfg), new BddBuilder()).compile();
        List<Condition> conditions = Arrays.asList(cfg.getConditions());

        // With A having 90% true probability:
        // - Condition A reach prob = 1.0
        // - Condition B reach prob = 0.9 (only if A is true)
        // Expected cost = 1.0 * 1 + 0.9 * 1 = 1.9
        BddCostEstimator estimator = new BddCostEstimator(
                conditions,
                ConditionCostModel.createUniform(),
                c -> c.toString().contains("booleanEquals(A") ? 0.9 : 0.5);
        double cost = estimator.expectedCost(bdd, conditions);

        assertEquals(1.9, cost, 0.001);
    }

    @Test
    void reachProbabilitiesForTerminalBdd() {
        Bdd bdd = new Bdd(1, 0, 1, 1, consumer -> {
            consumer.accept(-1, 1, -1);
        });

        List<Condition> ordering = List.of();
        BddCostEstimator estimator = new BddCostEstimator(ordering, ConditionCostModel.createUniform(), null);

        double[] probs = estimator.reachProbabilities(bdd, ordering);
        assertEquals(0, probs.length);
    }

    @Test
    void reachProbabilitiesForSingleCondition() {
        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder()
                        .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                        .build())
                .addRule(EndpointRule.builder()
                        .conditions(Condition.builder().fn(TestHelpers.isSet("Region")).build())
                        .endpoint(TestHelpers.endpoint("https://example.com")))
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        Bdd bdd = new BddCompiler(cfg, OrderingStrategy.initialOrdering(cfg), new BddBuilder()).compile();
        List<Condition> conditions = Arrays.asList(cfg.getConditions());

        BddCostEstimator estimator = new BddCostEstimator(conditions, ConditionCostModel.createUniform(), null);
        double[] probs = estimator.reachProbabilities(bdd, conditions);

        // Root condition always has reach probability 1.0
        assertEquals(1.0, probs[0], 0.001);
    }

    @Test
    void expectedCostWithDifferentCosts() {
        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder()
                        .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                        .addParameter(Parameter.builder()
                                .name("Endpoint")
                                .type(ParameterType.STRING)
                                .required(true)
                                .build())
                        .build())
                .addRule(EndpointRule.builder()
                        .conditions(
                                Condition.builder().fn(TestHelpers.isSet("Region")).build(),
                                Condition.builder().fn(TestHelpers.parseUrl("Endpoint")).result("url").build())
                        .endpoint(TestHelpers.endpoint("https://example.com")))
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        Bdd bdd = new BddCompiler(cfg, OrderingStrategy.initialOrdering(cfg), new BddBuilder()).compile();
        List<Condition> conditions = Arrays.asList(cfg.getConditions());

        // With default cost model: isSet=8, parseUrl=200
        // With uniform 0.5 probability:
        // - isSet reach = 1.0, cost contribution = 8
        // - parseUrl reach = 0.5 (only if isSet is true), cost contribution = 0.5 * 200 = 100
        // Total = 108
        BddCostEstimator estimator = new BddCostEstimator(conditions, ConditionCostModel.createDefault(), null);
        double cost = estimator.expectedCost(bdd, conditions);

        assertEquals(108.0, cost, 0.001);
    }
}
