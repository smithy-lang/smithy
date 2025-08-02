/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
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
import software.amazon.smithy.rulesengine.logic.TestHelpers;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;

// Does some basic checks, but doesn't get too specific so we can easily change the sifting algorithm.
class SiftingOptimizationTest {

    @Test
    void testBasicOptimization() {
        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("A").type(ParameterType.STRING).build())
                .addParameter(Parameter.builder().name("B").type(ParameterType.STRING).build())
                .addParameter(Parameter.builder().name("C").type(ParameterType.STRING).build())
                .build();

        Rule rule = EndpointRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("A")).build(),
                        Condition.builder().fn(TestHelpers.isSet("B")).build(),
                        Condition.builder().fn(TestHelpers.isSet("C")).build())
                .endpoint(TestHelpers.endpoint("https://example.com"));

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule)
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        BddTrait originalTrait = BddTrait.from(cfg);

        SiftingOptimization optimizer = SiftingOptimization.builder().cfg(cfg).build();
        BddTrait optimizedTrait = optimizer.apply(originalTrait);

        // Basic checks
        assertEquals(originalTrait.getConditions().size(), optimizedTrait.getConditions().size());
        assertEquals(originalTrait.getResults().size(), optimizedTrait.getResults().size());
        assertEquals(originalTrait.getBdd().getConditionCount(), optimizedTrait.getBdd().getConditionCount());
        assertEquals(originalTrait.getBdd().getResultCount(), optimizedTrait.getBdd().getResultCount());

        // Size should be same or smaller
        assertTrue(optimizedTrait.getBdd().getNodeCount() <= originalTrait.getBdd().getNodeCount());
    }

    @Test
    void testDependenciesPreserved() {
        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("Input").type(ParameterType.STRING).build())
                .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                .build();

        Rule rule = EndpointRule.builder()
                .conditions(Condition.builder()
                        .fn(TestHelpers.isSet("Input"))
                        .result(Identifier.of("hasInput"))
                        .build(),
                        Condition.builder()
                                .fn(BooleanEquals.ofExpressions(
                                        Expression.getReference(Identifier.of("hasInput")),
                                        Literal.of(true)))
                                .build(),
                        Condition.builder()
                                .fn(TestHelpers.isSet("Region"))
                                .build())
                .endpoint(TestHelpers.endpoint("https://example.com"));

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule)
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        BddTrait originalTrait = BddTrait.from(cfg);

        SiftingOptimization optimizer = SiftingOptimization.builder().cfg(cfg).build();
        BddTrait optimizedTrait = optimizer.apply(originalTrait);

        // Verify the optimizer preserved the number of conditions
        assertEquals(originalTrait.getConditions().size(), optimizedTrait.getConditions().size());
        assertEquals(originalTrait.getBdd().getConditionCount(), optimizedTrait.getBdd().getConditionCount());

        // The fact that the optimization completes successfully and produces a valid BDD
        // means dependencies were preserved (otherwise the BddCompiler would have failed
        // during the optimization process).

        // Also verify results are preserved
        assertEquals(originalTrait.getResults(), optimizedTrait.getResults());
    }

    @Test
    void testSingleCondition() {
        // Test a single condition edge case
        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                .build();

        Rule rule = EndpointRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("Region")).build())
                .endpoint(TestHelpers.endpoint("https://example.com"));

        EndpointRuleSet ruleSet = EndpointRuleSet.builder().parameters(params).addRule(rule).build();
        Cfg cfg = Cfg.from(ruleSet);
        BddTrait originalTrait = BddTrait.from(cfg);

        SiftingOptimization optimizer = SiftingOptimization.builder().cfg(cfg).build();
        BddTrait optimizedTrait = optimizer.apply(originalTrait);

        // Should be unchanged or very similar
        assertEquals(originalTrait.getBdd().getNodeCount(), optimizedTrait.getBdd().getNodeCount());
        assertEquals(1, optimizedTrait.getBdd().getConditionCount());
        assertEquals(originalTrait.getConditions(), optimizedTrait.getConditions());
    }

    @Test
    void testEmptyRuleSet() {
        // Test empty ruleset edge case
        Parameters params = Parameters.builder().build();
        EndpointRuleSet ruleSet = EndpointRuleSet.builder().parameters(params).build();

        Cfg cfg = Cfg.from(ruleSet);
        BddTrait originalTrait = BddTrait.from(cfg);

        SiftingOptimization optimizer = SiftingOptimization.builder().cfg(cfg).build();
        BddTrait optimizedTrait = optimizer.apply(originalTrait);

        assertEquals(0, optimizedTrait.getBdd().getConditionCount());
        assertEquals(originalTrait.getBdd().getResultCount(), optimizedTrait.getBdd().getResultCount());
        assertEquals(originalTrait.getResults(), optimizedTrait.getResults());
    }

    @Test
    void testLargeReduction() {
        // Create a ruleset that should benefit from optimization
        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("A").type(ParameterType.STRING).build())
                .addParameter(Parameter.builder().name("B").type(ParameterType.STRING).build())
                .addParameter(Parameter.builder().name("C").type(ParameterType.STRING).build())
                .addParameter(Parameter.builder().name("D").type(ParameterType.STRING).build())
                .build();

        // Multiple rules with overlapping conditions
        Rule rule1 = EndpointRule.builder()
                .conditions(
                        Condition.builder().fn(TestHelpers.isSet("A")).build(),
                        Condition.builder().fn(TestHelpers.isSet("B")).build())
                .endpoint(TestHelpers.endpoint("https://ab.example.com"));

        Rule rule2 = EndpointRule.builder()
                .conditions(
                        Condition.builder().fn(TestHelpers.isSet("A")).build(),
                        Condition.builder().fn(TestHelpers.isSet("C")).build())
                .endpoint(TestHelpers.endpoint("https://ac.example.com"));

        Rule rule3 = EndpointRule.builder()
                .conditions(
                        Condition.builder().fn(TestHelpers.isSet("B")).build(),
                        Condition.builder().fn(TestHelpers.isSet("D")).build())
                .endpoint(TestHelpers.endpoint("https://bd.example.com"));

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .addRule(rule1)
                .addRule(rule2)
                .addRule(rule3)
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        BddTrait originalTrait = BddTrait.from(cfg);

        SiftingOptimization optimizer = SiftingOptimization.builder()
                .cfg(cfg)
                .granularEffort(100_000, 10) // Allow more aggressive optimization
                .build();
        BddTrait optimizedTrait = optimizer.apply(originalTrait);

        // Should maintain correctness
        assertEquals(originalTrait.getConditions().size(), optimizedTrait.getConditions().size());
        assertEquals(originalTrait.getBdd().getConditionCount(), optimizedTrait.getBdd().getConditionCount());
        assertEquals(originalTrait.getBdd().getResultCount(), optimizedTrait.getBdd().getResultCount());
        assertEquals(originalTrait.getResults(), optimizedTrait.getResults());

        // Often achieves some reduction
        assertTrue(optimizedTrait.getBdd().getNodeCount() <= originalTrait.getBdd().getNodeCount());
    }

    @Test
    void testNoImprovementReturnsOriginal() {
        // Test that when no improvement is found, the original trait is returned
        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                .build();

        Rule rule = EndpointRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("Region")).build())
                .endpoint(TestHelpers.endpoint("https://example.com"));

        EndpointRuleSet ruleSet = EndpointRuleSet.builder().parameters(params).addRule(rule).build();
        Cfg cfg = Cfg.from(ruleSet);
        BddTrait originalTrait = BddTrait.from(cfg);

        SiftingOptimization optimizer = SiftingOptimization.builder()
                .cfg(cfg)
                .coarseEffort(1, 1) // Minimal effort to likely find no improvement
                .build();

        BddTrait optimizedTrait = optimizer.apply(originalTrait);

        // For simple cases with minimal optimization effort, should return the same trait object
        if (optimizedTrait.getBdd().getNodeCount() == originalTrait.getBdd().getNodeCount()) {
            assertTrue(optimizedTrait == originalTrait);
        }
    }
}
