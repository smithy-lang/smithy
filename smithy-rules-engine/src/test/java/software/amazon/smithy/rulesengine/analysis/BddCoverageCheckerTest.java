/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.analysis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.BooleanEquals;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.logic.TestHelpers;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;
import software.amazon.smithy.rulesengine.traits.EndpointBddTrait;
import software.amazon.smithy.rulesengine.traits.EndpointTestCase;
import software.amazon.smithy.rulesengine.traits.EndpointTestExpectation;
import software.amazon.smithy.rulesengine.traits.ExpectedEndpoint;
import software.amazon.smithy.utils.ListUtils;
import software.amazon.smithy.utils.MapUtils;

public class BddCoverageCheckerTest {

    private EndpointBddTrait bddTrait;
    private BddCoverageChecker checker;

    @BeforeEach
    void setUp() {
        // Create a simple ruleset with multiple conditions and results
        Parameters parameters = Parameters.builder()
                .addParameter(Parameter.builder()
                        .name("Region")
                        .type(ParameterType.STRING)
                        .required(false)
                        .build())
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
                .build();

        List<Rule> rules = new ArrayList<>();

        // Rule 1: If Region is set and UseFips is true, error
        rules.add(ErrorRule.builder()
                .conditions(ListUtils.of(
                        Condition.builder()
                                .fn(IsSet.ofExpressions(Expression.getReference("Region")))
                                .build(),
                        Condition.builder()
                                .fn(BooleanEquals.ofExpressions(Expression.getReference("UseFips"), true))
                                .build()))
                .error("FIPS not supported in this region"));

        // Rule 2: If Region is set and UseDualStack is true, specific endpoint
        rules.add(EndpointRule.builder()
                .conditions(ListUtils.of(
                        Condition.builder()
                                .fn(IsSet.ofExpressions(Expression.getReference("Region")))
                                .build(),
                        Condition.builder()
                                .fn(BooleanEquals.ofExpressions(Expression.getReference("UseDualStack"), true))
                                .build()))
                .endpoint(TestHelpers.endpoint("https://dualstack.example.com")));

        // Rule 3: Default endpoint
        rules.add(EndpointRule.builder().endpoint(TestHelpers.endpoint("https://empty.com")));

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(parameters)
                .rules(rules)
                .version("1.1")
                .build();

        // Convert to BDD
        Cfg cfg = Cfg.from(ruleSet);
        bddTrait = EndpointBddTrait.from(cfg);
        checker = new BddCoverageChecker(bddTrait);
    }

    @Test
    void testInitialCoverage() {
        assertEquals(0.0, checker.getConditionCoverage());
        assertEquals(0.0, checker.getResultCoverage());
    }

    @Test
    void testSingleTestCaseCoverage() {
        // Test with Region set and UseFips true
        EndpointTestCase testCase = EndpointTestCase.builder()
                .params(ObjectNode.builder()
                        .withMember("Region", "us-east-1")
                        .withMember("UseFips", true)
                        .withMember("UseDualStack", false)
                        .build())
                .expect(EndpointTestExpectation.builder()
                        .error("FIPS not supported in this region")
                        .build())
                .build();

        checker.evaluateTestCase(testCase);

        // Should have covered some conditions
        assertTrue(checker.getConditionCoverage() > 0.0);
        assertTrue(checker.getResultCoverage() > 0.0);

        // Should have fewer unevaluated conditions
        Set<Condition> unevaluatedConditions = checker.getUnevaluatedConditions();
        assertTrue(unevaluatedConditions.size() < bddTrait.getConditions().size());
    }

    @Test
    void testMultipleTestCasesCoverage() {
        // Test case 1: FIPS error path
        checker.evaluateTestCase(EndpointTestCase.builder()
                .params(ObjectNode.builder()
                        .withMember("Region", "us-east-1")
                        .withMember("UseFips", true)
                        .withMember("UseDualStack", false)
                        .build())
                .expect(EndpointTestExpectation.builder()
                        .error("FIPS not supported in this region")
                        .build())
                .build());

        double coverageAfterFirst = checker.getConditionCoverage();

        // Test case 2: Dual stack path
        checker.evaluateTestCase(EndpointTestCase.builder()
                .params(ObjectNode.builder()
                        .withMember("Region", "us-east-1")
                        .withMember("UseFips", false)
                        .withMember("UseDualStack", true)
                        .build())
                .expect(EndpointTestExpectation.builder()
                        .endpoint(ExpectedEndpoint.builder()
                                .url("https://dualstack.example.com")
                                .build())
                        .build())
                .build());

        double coverageAfterSecond = checker.getConditionCoverage();

        // Coverage should increase or stay the same
        assertTrue(coverageAfterSecond >= coverageAfterFirst);

        // Test case 3: Default endpoint
        checker.evaluateTestCase(EndpointTestCase.builder()
                .params(ObjectNode.builder()
                        .withMember("UseFips", false)
                        .withMember("UseDualStack", false)
                        .build())
                .expect(EndpointTestExpectation.builder()
                        .endpoint(ExpectedEndpoint.builder()
                                .url("https://example.com")
                                .build())
                        .build())
                .build());

        double coverageAfterThird = checker.getConditionCoverage();
        assertTrue(coverageAfterThird >= coverageAfterSecond);
    }

    @Test
    void testEvaluateInput() {
        // Test direct input evaluation
        checker.evaluateInput(MapUtils.of(
                Identifier.of("Region"),
                Value.stringValue("us-west-2"),
                Identifier.of("UseFips"),
                Value.booleanValue(false),
                Identifier.of("UseDualStack"),
                Value.booleanValue(true)));

        assertTrue(checker.getConditionCoverage() > 0.0);
        assertTrue(checker.getResultCoverage() > 0.0);
    }

    @Test
    void testFullCoverage() {
        List<EndpointTestCase> testCases = ListUtils.of(
                // FIPS error
                EndpointTestCase.builder()
                        .params(ObjectNode.builder()
                                .withMember("Region", "us-east-1")
                                .withMember("UseFips", true)
                                .build())
                        .expect(EndpointTestExpectation.builder()
                                .error("FIPS not supported in this region")
                                .build())
                        .build(),
                // Dual stack
                EndpointTestCase.builder()
                        .params(ObjectNode.builder()
                                .withMember("Region", "us-east-1")
                                .withMember("UseDualStack", true)
                                .build())
                        .expect(EndpointTestExpectation.builder()
                                .endpoint(ExpectedEndpoint.builder()
                                        .url("https://dualstack.example.com")
                                        .build())
                                .build())
                        .build(),
                // Default with region
                EndpointTestCase.builder()
                        .params(ObjectNode.builder()
                                .withMember("Region", "us-east-1")
                                .withMember("UseFips", false)
                                .withMember("UseDualStack", false)
                                .build())
                        .expect(EndpointTestExpectation.builder()
                                .endpoint(ExpectedEndpoint.builder()
                                        .url("https://example.com")
                                        .build())
                                .build())
                        .build(),
                // Default without region
                EndpointTestCase.builder()
                        .params(ObjectNode.objectNode())
                        .expect(EndpointTestExpectation.builder()
                                .endpoint(ExpectedEndpoint.builder()
                                        .url("https://example.com")
                                        .build())
                                .build())
                        .build());

        for (EndpointTestCase testCase : testCases) {
            checker.evaluateTestCase(testCase);
        }

        assertEquals(100.0, checker.getConditionCoverage());
        assertEquals(100.0, checker.getResultCoverage());
    }

    @Test
    void testEmptyBdd() {
        Parameters emptyParams = Parameters.builder().build();
        EndpointRuleSet emptyRuleSet = EndpointRuleSet.builder()
                .parameters(emptyParams)
                .rules(ListUtils.of(EndpointRule.builder().endpoint(TestHelpers.endpoint("https://empty.com"))))
                .version("1.1")
                .build();

        Cfg emptyCfg = Cfg.from(emptyRuleSet);
        EndpointBddTrait emptyBdd = EndpointBddTrait.from(emptyCfg);
        BddCoverageChecker emptyChecker = new BddCoverageChecker(emptyBdd);

        assertEquals(100.0, emptyChecker.getConditionCoverage(), 0.01);
        assertEquals(0.0, emptyChecker.getResultCoverage(), 0.01);

        emptyChecker.evaluateInput(MapUtils.of());
        assertTrue(emptyChecker.getResultCoverage() > 0.0);
    }
}
