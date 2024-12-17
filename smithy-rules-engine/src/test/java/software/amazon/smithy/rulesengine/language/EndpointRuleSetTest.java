/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.evaluation.RuleEvaluator;
import software.amazon.smithy.rulesengine.language.evaluation.value.EndpointValue;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Reference;
import software.amazon.smithy.rulesengine.language.syntax.expressions.literal.Literal;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.rulesengine.traits.EndpointRuleSetTrait;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.MapUtils;

public class EndpointRuleSetTest {
    private static final Endpoint MINIMAL_ENDPOINT = Endpoint.builder()
            .sourceLocation(SourceLocation.none())
            .url(Literal.of("https://{Region}.amazonaws.com"))
            .addAuthScheme(Identifier.of("sigv4"),
                    MapUtils.of(
                            Identifier.of("signingRegion"),
                            Literal.of("{Region}"),
                            Identifier.of("signingName"),
                            Literal.of("serviceName")))
            .build();

    @Test
    public void testRuleEval() {
        EndpointRuleSet actual = TestRunnerTest.getMinimalEndpointRuleSet();
        Value result = RuleEvaluator.evaluate(actual,
                MapUtils.of(Identifier.of("Region"),
                        Value.stringValue("us-east-1")));
        EndpointValue expected = new EndpointValue.Builder(SourceLocation.none())
                .url("https://us-east-1.amazonaws.com")
                .putProperty("authSchemes",
                        Value.arrayValue(Collections.singletonList(
                                Value.recordValue(MapUtils.of(
                                        Identifier.of("name"),
                                        Value.stringValue("sigv4"),
                                        Identifier.of("signingRegion"),
                                        Value.stringValue("us-east-1"),
                                        Identifier.of("signingName"),
                                        Value.stringValue("serviceName"))))))
                .build();
        assertEquals(expected, result.expectEndpointValue());
    }

    @Test
    public void testDeterministicSerde() {
        EndpointRuleSet actual = TestRunnerTest.getMinimalEndpointRuleSet();
        String asString = IoUtils.readUtf8Resource(EndpointRuleSetTest.class, "minimal-ruleset.json");
        assertEquals(Node.prettyPrintJson(Node.parseJsonWithComments(asString)), Node.prettyPrintJson(actual.toNode()));
    }

    @Test
    public void testMinimalRuleset() {
        EndpointRuleSet actual = TestRunnerTest.getMinimalEndpointRuleSet();
        assertEquals(EndpointRuleSet.builder()
                .version("1.3")
                .parameters(Parameters
                        .builder()
                        .addParameter(Parameter.builder()
                                .name("Region")
                                .builtIn("AWS::Region")
                                .type(ParameterType.STRING)
                                .required(true)
                                .build())
                        .build())
                .addRule(Rule
                        .builder()
                        .description("base rule")
                        .endpoint(MINIMAL_ENDPOINT))
                .build(), actual);
    }

    @Test
    public void testEndpointCollectorMinimalRuleset() {
        EndpointRuleSet ers = TestRunnerTest.getMinimalEndpointRuleSet();
        Map<String, Endpoint> actual = EndpointRuleSet.EndpointPathCollector
                .from(EndpointRuleSetTrait.builder()
                        .ruleSet(ers.toNode())
                        .build())
                .collect();
        Map<String, Endpoint> expected = MapUtils.of("/rules/0/endpoint", MINIMAL_ENDPOINT);
        assertEquals(expected, actual);
    }

    @Test
    public void testEndpointCollectorEmptyRuleset() {
        EndpointRuleSet ers = EndpointRuleSet.builder()
                .parameters(Parameters.builder().build())
                .build();
        Map<String, Endpoint> actual = EndpointRuleSet.EndpointPathCollector
                .from(EndpointRuleSetTrait.builder()
                        .ruleSet(ers.toNode())
                        .build())
                .collect();
        Map<String, Endpoint> expected = Collections.emptyMap();
        assertEquals(expected, actual);
    }

    @Test
    public void testEndpointCollectorComplexRuleset() {
        EndpointRuleSet ers = TestRunnerTest.getEndpointRuleSet(
                EndpointRuleSetTest.class,
                "example-complex-ruleset.json");
        Map<String, Endpoint> actual = EndpointRuleSet.EndpointPathCollector
                .from(EndpointRuleSetTrait.builder()
                        .ruleSet(ers.toNode())
                        .build())
                .collect();
        Map<String, Endpoint> expected = MapUtils.of(
                "/rules/1/rules/0/rules/0/rules/1/endpoint",
                Endpoint.builder()
                        .url(Literal.of("https://example.{Region}.dual-stack-dns-suffix.com"))
                        .build(),
                "/rules/1/rules/0/rules/1/rules/0/rules/0/endpoint",
                Endpoint.builder()
                        .url(Literal.of("https://example-fips.{Region}.dual-stack-dns-suffix.com"))
                        .build(),
                "/rules/1/rules/0/rules/2/endpoint",
                Endpoint.builder()
                        .url(Literal.of("https://example.{Region}.dual-stack-dns-suffix.com"))
                        .build(),
                "/rules/0/rules/1/endpoint",
                Endpoint.builder()
                        .url(new Reference(Identifier.of("Endpoint"), SourceLocation.NONE))
                        .build(),
                "/rules/1/rules/0/rules/0/rules/0/rules/0/rules/0/endpoint",
                Endpoint.builder()
                        .url(Literal.of("https://example-fips.{Region}.dual-stack-dns-suffix.com"))
                        .build());
        assertEquals(expected, actual);
    }
}
