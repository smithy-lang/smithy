/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.language.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.TestRunnerTest;
import software.amazon.smithy.rulesengine.language.evaluation.value.EndpointValue;
import software.amazon.smithy.rulesengine.language.evaluation.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.utils.MapUtils;

public class RuleEngineTest {
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
}
