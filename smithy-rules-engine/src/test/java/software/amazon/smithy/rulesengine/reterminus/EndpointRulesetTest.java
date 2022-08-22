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

package software.amazon.smithy.rulesengine.reterminus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.InputStream;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.rulesengine.reterminus.eval.RuleEngine;
import software.amazon.smithy.rulesengine.reterminus.eval.Scope;
import software.amazon.smithy.rulesengine.reterminus.eval.Value;
import software.amazon.smithy.rulesengine.reterminus.lang.Identifier;
import software.amazon.smithy.rulesengine.reterminus.lang.expr.Literal;
import software.amazon.smithy.rulesengine.reterminus.lang.parameters.Parameter;
import software.amazon.smithy.rulesengine.reterminus.lang.parameters.ParameterType;
import software.amazon.smithy.rulesengine.reterminus.lang.parameters.Parameters;
import software.amazon.smithy.rulesengine.reterminus.lang.rule.Rule;
import software.amazon.smithy.utils.MapUtils;

class EndpointRulesetTest {
    private EndpointRuleset parse(String resource) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resource);
        assert is != null;
        Node node = ObjectNode.parse(is);
        return EndpointRuleset.fromNode(node);
    }

    @Test
    void testRuleEval() {
        EndpointRuleset actual = parse(
                "software/amazon/smithy/rulesengine/testutil/valid-rules/minimal-ruleset.json");
        Value result = RuleEngine.evaluate(actual, MapUtils.of(Identifier.of("Region"), Value.str("us-east-1")));
        Value.Endpoint expected = new Value.Endpoint.Builder(SourceLocation.none())
                .url("https://us-east-1.amazonaws.com")
                .addProperty("authSchemes", Value.array(Collections.singletonList(
                        Value.record(MapUtils.of(
                                Identifier.of("name"), Value.str("sigv4"),
                                Identifier.of("signingRegion"), Value.str("us-east-1"),
                                Identifier.of("signingName"), Value.str("serviceName")
                        ))
                )))
                .build();
        assertEquals(expected, result.expectEndpoint());
    }

    @Test
    void testMinimalRuleset() {
        EndpointRuleset actual = parse(
                "software/amazon/smithy/rulesengine/testutil/valid-rules/minimal-ruleset.json");
        actual.typecheck(new Scope<>());
        assertEquals(EndpointRuleset.builder()
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
                                .builder(SourceLocation.none())
                                .url(Literal.of("https://{Region}.amazonaws.com"))
                                .addAuthScheme(Identifier.of("sigv4"), MapUtils.of(
                                        Identifier.of("signingRegion"), Literal.of("{Region}"),
                                        Identifier.of("signingName"), Literal.of("serviceName")))
                                .build()))
                .build(), actual
        );
    }
}
