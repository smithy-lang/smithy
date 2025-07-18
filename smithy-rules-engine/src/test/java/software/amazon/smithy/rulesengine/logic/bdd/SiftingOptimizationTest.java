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
        Bdd originalBdd = Bdd.from(cfg);

        SiftingOptimization optimizer = SiftingOptimization.builder().cfg(cfg).build();
        Bdd optimizedBdd = optimizer.apply(originalBdd);

        // Basic checks
        assertEquals(originalBdd.getConditions().size(), optimizedBdd.getConditions().size());
        assertEquals(originalBdd.getResults().size(), optimizedBdd.getResults().size());

        // Size should be same or smaller
        assertTrue(optimizedBdd.getNodes().length <= originalBdd.getNodes().length);
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
        Bdd originalBdd = Bdd.from(cfg);

        SiftingOptimization optimizer = SiftingOptimization.builder().cfg(cfg).build();
        Bdd optimizedBdd = optimizer.apply(originalBdd);

        // Find the positions of dependent conditions
        int hasInputPos = -1;
        int usesInputPos = -1;
        for (int i = 0; i < optimizedBdd.getConditions().size(); i++) {
            Condition cond = optimizedBdd.getConditions().get(i);
            if (cond.getResult().isPresent() &&
                    cond.getResult().get().toString().equals("hasInput")) {
                hasInputPos = i;
            } else if (cond.getFunction().toString().contains("hasInput")) {
                usesInputPos = i;
            }
        }

        // Verify dependency is preserved: definer comes before user
        assertTrue(hasInputPos < usesInputPos,
                "Condition defining hasInput must come before condition using it");
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
        Bdd originalBdd = Bdd.from(cfg);

        SiftingOptimization optimizer = SiftingOptimization.builder().cfg(cfg).build();
        Bdd optimizedBdd = optimizer.apply(originalBdd);

        // Should be unchanged
        assertEquals(originalBdd.getNodes().length, optimizedBdd.getNodes().length);
        assertEquals(1, optimizedBdd.getConditions().size());
    }
}
