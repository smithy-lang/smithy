/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.logic.cfg;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.functions.IsSet;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Condition;
import software.amazon.smithy.rulesengine.language.syntax.rule.EndpointRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.ErrorRule;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.language.syntax.rule.TreeRule;
import software.amazon.smithy.rulesengine.logic.TestHelpers;
import software.amazon.smithy.utils.ListUtils;

/**
 * Tests that isSet(f(x)) + v = f(x) patterns are consolidated to eliminate redundant function calls.
 */
class IsSetUnwrapTransformTest {

    @Test
    void isSetParseUrlConsolidatedWithBinding() {
        // Pattern: isSet(Endpoint) -> isSet(parseURL(Endpoint)) -> url = parseURL(Endpoint)
        // The isSet(parseURL(Endpoint)) is wasteful - the binding itself acts as the null check
        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("Endpoint").type(ParameterType.STRING).build())
                .build();

        // isSet(Endpoint) - proves Endpoint is non-null
        Condition isSetEndpoint = Condition.builder()
                .fn(TestHelpers.isSet("Endpoint"))
                .build();

        // isSet(parseURL(Endpoint)) - bare check (wasteful)
        Condition isSetParseUrl = Condition.builder()
                .fn(IsSet.ofExpressions(TestHelpers.parseUrl("Endpoint")))
                .build();

        // url = parseURL(Endpoint) - binding
        Condition urlBinding = Condition.builder()
                .fn(TestHelpers.parseUrl("Endpoint"))
                .result(Identifier.of("url"))
                .build();

        Rule innerRule = EndpointRule.builder()
                .conditions(urlBinding)
                .endpoint(TestHelpers.endpoint("https://example.com"));

        Rule parseUrlTree = TreeRule.builder()
                .conditions(isSetParseUrl)
                .treeRule(ListUtils.of(innerRule));

        Rule endpointTree = TreeRule.builder()
                .conditions(isSetEndpoint)
                .treeRule(ListUtils.of(parseUrlTree));

        Rule errorRule = ErrorRule.builder().error("No endpoint");

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .rules(ListUtils.of(endpointTree, errorRule))
                .build();

        // Build CFG - transforms should consolidate the redundant parseURL calls
        Cfg cfg = Cfg.from(ruleSet);

        // Should have only ONE parseURL condition, not two
        long parseUrlCount = Arrays.stream(cfg.getConditions())
                .filter(c -> c.getFunction().getName().equals("parseURL"))
                .count();

        System.out.println("=== Test: isSetParseUrlConsolidatedWithBinding ===");
        System.out.println("Conditions after transform (" + cfg.getConditions().length + " total):");
        for (Condition c : cfg.getConditions()) {
            System.out.println("  " + c.getResult().map(r -> r + " = ").orElse("") + c.getFunction());
        }
        System.out.println("parseURL count: " + parseUrlCount);

        assertEquals(1, parseUrlCount, "Should have exactly one parseURL condition after consolidation");
    }

    @Test
    void multipleIsSetFunctionCallsConsolidated() {
        // Pattern: isSet(Endpoint) -> isSet(parseURL(Endpoint)) -> url = parseURL(Endpoint)
        Parameters params = Parameters.builder()
                .addParameter(Parameter.builder().name("Endpoint").type(ParameterType.STRING).build())
                .addParameter(Parameter.builder().name("Region").type(ParameterType.STRING).build())
                .build();

        // isSet(Endpoint) proves Endpoint is non-null
        Condition isSetEndpoint = Condition.builder()
                .fn(TestHelpers.isSet("Endpoint"))
                .build();

        // isSet(parseURL(Endpoint)) - wasteful check
        Condition isSetParseUrl = Condition.builder()
                .fn(IsSet.ofExpressions(TestHelpers.parseUrl("Endpoint")))
                .build();

        // url = parseURL(Endpoint) - binding
        Condition urlBinding = Condition.builder()
                .fn(TestHelpers.parseUrl("Endpoint"))
                .result(Identifier.of("url"))
                .build();

        Rule innerEndpoint = EndpointRule.builder()
                .conditions(urlBinding)
                .endpoint(TestHelpers.endpoint("https://custom.com"));

        Rule parseUrlTree = TreeRule.builder()
                .conditions(isSetParseUrl)
                .treeRule(ListUtils.of(innerEndpoint));

        Rule branch1 = TreeRule.builder()
                .conditions(isSetEndpoint)
                .treeRule(ListUtils.of(parseUrlTree));

        // Branch 2: isSet(Region) -> different endpoint
        Rule branch2 = EndpointRule.builder()
                .conditions(Condition.builder().fn(TestHelpers.isSet("Region")).build())
                .endpoint(TestHelpers.endpoint("https://regional.com"));

        Rule errorRule = ErrorRule.builder().error("No endpoint");

        EndpointRuleSet ruleSet = EndpointRuleSet.builder()
                .parameters(params)
                .rules(ListUtils.of(branch1, branch2, errorRule))
                .build();

        Cfg cfg = Cfg.from(ruleSet);

        System.out.println("Conditions after transform:");
        for (Condition c : cfg.getConditions()) {
            System.out.println("  " + c.getResult().map(r -> r + " = ").orElse("") + c.getFunction());
        }

        // Should have only ONE parseURL condition
        long parseUrlCount = Arrays.stream(cfg.getConditions())
                .filter(c -> c.getFunction().getName().equals("parseURL"))
                .count();

        assertEquals(1, parseUrlCount, "Should have exactly one parseURL condition");
    }
}
