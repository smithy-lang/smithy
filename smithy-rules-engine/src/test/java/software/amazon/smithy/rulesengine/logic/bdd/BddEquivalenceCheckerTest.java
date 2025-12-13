/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.bdd;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.logic.TestHelpers;
import software.amazon.smithy.rulesengine.logic.cfg.Cfg;
import software.amazon.smithy.utils.ListUtils;

class BddEquivalenceCheckerTest {

    @Test
    void testSimpleEquivalentBdd() {
        // Create a simple ruleset
        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder()
                        .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                        .build())
                .rules(ListUtils.of(
                        EndpointRule.builder()
                                .conditions(Condition.builder().fn(TestHelpers.isSet("Region")).build())
                                .endpoint(TestHelpers.endpoint("https://example.com")),
                        // Default case
                        ErrorRule.builder().error(Literal.of("No region provided"))))
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        BddCompiler compiler = new BddCompiler(cfg, new BddBuilder());
        Bdd bdd = compiler.compile();

        BddEquivalenceChecker checker = BddEquivalenceChecker.of(
                cfg,
                bdd,
                compiler.getOrderedConditions(),
                compiler.getIndexedResults());

        assertDoesNotThrow(checker::verify);
    }

    @Test
    void testEmptyRulesetEquivalence() {
        // Empty ruleset with a default endpoint
        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder().build())
                .rules(ListUtils.of(EndpointRule.builder().endpoint(TestHelpers.endpoint("https://default.com"))))
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        BddCompiler compiler = new BddCompiler(cfg, new BddBuilder());
        Bdd bdd = compiler.compile();

        BddEquivalenceChecker checker = BddEquivalenceChecker.of(
                cfg,
                bdd,
                compiler.getOrderedConditions(),
                compiler.getIndexedResults());

        assertDoesNotThrow(checker::verify);
    }

    @Test
    void testMultipleConditionsEquivalence() {
        // Ruleset with multiple conditions (AND logic)
        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder()
                        .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                        .addParameter(Parameter.builder().name("Bucket").type(ParameterType.STRING).build())
                        .build())
                .rules(ListUtils.of(
                        EndpointRule.builder()
                                .conditions(
                                        Condition.builder().fn(TestHelpers.isSet("Region")).build(),
                                        Condition.builder().fn(TestHelpers.isSet("Bucket")).build())
                                .endpoint(TestHelpers.endpoint("https://example.com")),
                        ErrorRule.builder().error(Literal.of("Missing required parameters"))))
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        BddCompiler compiler = new BddCompiler(cfg, new BddBuilder());
        Bdd bdd = compiler.compile();

        BddEquivalenceChecker checker = BddEquivalenceChecker.of(
                cfg,
                bdd,
                compiler.getOrderedConditions(),
                compiler.getIndexedResults());

        assertDoesNotThrow(checker::verify);
    }

    @Test
    void testSetMaxSamples() {
        // Create a simpler test with just 3 parameters to avoid ordering issues
        Parameters.Builder paramsBuilder = Parameters.builder();
        List<Rule> rules = new ArrayList<>();

        // Add parameters with zero-padded names to ensure correct ordering
        for (int i = 0; i < 3; i++) {
            String paramName = String.format("Param%02d", i); // Param00, Param01, Param02
            paramsBuilder.addParameter(Parameter.builder().name(paramName).type(ParameterType.STRING).build());
            rules.add(EndpointRule.builder()
                    .conditions(Condition.builder().fn(TestHelpers.isSet(paramName)).build())
                    .endpoint(TestHelpers.endpoint("https://example" + i + ".com")));
        }

        // default case
        rules.add(ErrorRule.builder().error(Literal.of("No parameters set")));

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(paramsBuilder.build())
                .rules(rules)
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        BddCompiler compiler = new BddCompiler(cfg, new BddBuilder());
        Bdd bdd = compiler.compile();

        BddEquivalenceChecker checker = BddEquivalenceChecker.of(
                cfg,
                bdd,
                compiler.getOrderedConditions(),
                compiler.getIndexedResults());

        // Set a small max samples to make test fast
        checker.setMaxSamples(100);

        assertDoesNotThrow(checker::verify);
    }

    @Test
    void testSetMaxDuration() {
        // Create a complex ruleset
        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(Parameters.builder()
                        .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                        .build())
                .rules(ListUtils.of(
                        EndpointRule.builder()
                                .conditions(Condition.builder().fn(TestHelpers.isSet("Region")).build())
                                .endpoint(TestHelpers.endpoint("https://example.com")),
                        ErrorRule.builder().error(Literal.of("No region provided"))))
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        BddCompiler compiler = new BddCompiler(cfg, new BddBuilder());
        Bdd bdd = compiler.compile();

        BddEquivalenceChecker checker = BddEquivalenceChecker.of(
                cfg,
                bdd,
                compiler.getOrderedConditions(),
                compiler.getIndexedResults());

        // Set a short timeout
        checker.setMaxDuration(Duration.ofMillis(100));

        assertDoesNotThrow(checker::verify);
    }

    @Test
    void testLargeNumberOfConditions() {
        // Test with 25 conditions to ensure it uses sampling rather than exhaustive testing
        Parameters.Builder paramsBuilder = Parameters.builder();
        List<Condition> conditions = new ArrayList<>();

        for (int i = 0; i < 25; i++) {
            String paramName = String.format("Param%02d", i);
            paramsBuilder.addParameter(Parameter.builder().name(paramName).type(ParameterType.STRING).build());
            conditions.add(Condition.builder().fn(TestHelpers.isSet(paramName)).build());
        }

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(paramsBuilder.build())
                .rules(ListUtils.of(
                        EndpointRule.builder()
                                .conditions(conditions)
                                .endpoint(TestHelpers.endpoint("https://example.com")),
                        ErrorRule.builder().error(Literal.of("Not all parameters set"))))
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        BddCompiler compiler = new BddCompiler(cfg, new BddBuilder());
        Bdd bdd = compiler.compile();

        BddEquivalenceChecker checker = BddEquivalenceChecker.of(
                cfg,
                bdd,
                compiler.getOrderedConditions(),
                compiler.getIndexedResults());

        // Set reasonable limits for large condition sets
        checker.setMaxSamples(10000);
        checker.setMaxDuration(Duration.ofSeconds(5));

        assertDoesNotThrow(checker::verify);
    }

    @Test
    void testCfgConditionMappingError() {
        Parameters region = Parameters.builder()
                .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                .build();
        Rule endpointRule = EndpointRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("Region")).build())
                .endpoint(TestHelpers.endpoint("https://example.com"));
        Rule errorRule = ErrorRule.builder().error(Literal.of("No region provided"));
        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(region)
                .rules(ListUtils.of(endpointRule, errorRule))
                .build();

        Cfg cfg = Cfg.from(ruleSet);
        BddCompiler compiler = new BddCompiler(cfg, new BddBuilder());
        Bdd bdd = compiler.compile();

        // Create a different condition list that doesn't contain the CFG conditions
        List<Condition> differentConditions = ListUtils.of(
                Condition.builder().fn(TestHelpers.isSet("DifferentParam")).build());

        // This should throw IllegalStateException due to missing CFG condition mapping
        assertThrows(IllegalStateException.class, () -> {
            BddEquivalenceChecker.of(cfg, bdd, differentConditions, compiler.getIndexedResults());
        });
    }
}
