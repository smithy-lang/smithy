/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.rulesengine.language;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.RulesetTestUtil;
import software.amazon.smithy.rulesengine.language.eval.RuleEvaluator;
import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.rulesengine.language.syntax.expr.Literal;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameter;
import software.amazon.smithy.rulesengine.language.syntax.parameters.ParameterType;
import software.amazon.smithy.rulesengine.language.syntax.parameters.Parameters;
import software.amazon.smithy.rulesengine.language.syntax.rule.Rule;
import software.amazon.smithy.utils.IoUtils;
import software.amazon.smithy.utils.MapUtils;

class EndpointRuleSetTest {
    @Test
    void testRuleEval() {
        EndpointRuleSet actual = RulesetTestUtil.minimalRuleSet();
        Value result = RuleEvaluator.evaluate(actual, MapUtils.of(Identifier.of("Region"),
                Value.string("us-east-1")));
        Value.Endpoint expected = new Value.Endpoint.Builder(SourceLocation.none())
                .url("https://us-east-1.amazonaws.com")
                .addProperty("authSchemes", Value.array(Collections.singletonList(
                        Value.record(MapUtils.of(
                                Identifier.of("name"), Value.string("sigv4"),
                                Identifier.of("signingRegion"), Value.string("us-east-1"),
                                Identifier.of("signingName"), Value.string("serviceName")
                        ))
                )))
                .build();
        assertEquals(expected, result.expectEndpoint());
    }

    @Test
    void testDeterministicSerde() {
        String resourceId = "software/amazon/smithy/rulesengine/testutil/valid-rules/minimal-ruleset.json";
        EndpointRuleSet actual = RulesetTestUtil.loadRuleSet(resourceId);
        String asString = IoUtils.readUtf8Resource(RulesetTestUtil.class.getClassLoader(), resourceId);
        assertEquals(Node.prettyPrintJson(Node.parseJsonWithComments(asString)), Node.prettyPrintJson(actual.toNode()));
    }

    @Test
    void testMinimalRuleset() {
        EndpointRuleSet actual = RulesetTestUtil.minimalRuleSet();
        assertEquals(EndpointRuleSet.builder()
                .version("1.3")
                .parameters(Parameters
                        .builder()
                        .addParameter(Parameter
                                .builder()
                                .name("Region")
                                .builtIn("AWS::Region")
                                .type(ParameterType.STRING)
                                .required(true)
                        ).build())
                .addRule(Rule
                        .builder()
                        .description("base rule")
                        .endpoint(Endpoint
                                .builder()
                                .sourceLocation(SourceLocation.none())
                                .url(Literal.of("https://{Region}.amazonaws.com"))
                                .addAuthScheme(Identifier.of("sigv4"), MapUtils.of(
                                        Identifier.of("signingRegion"), Literal.of("{Region}"),
                                        Identifier.of("signingName"), Literal.of("serviceName")))
                                .build()))
                .build(), actual
        );
    }
}
