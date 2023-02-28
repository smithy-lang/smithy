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

package software.amazon.smithy.rulesengine.language.eval;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.SourceLocation;
import software.amazon.smithy.rulesengine.language.EndpointRuleSet;
import software.amazon.smithy.rulesengine.language.TestDiscovery;
import software.amazon.smithy.rulesengine.language.eval.value.EndpointValue;
import software.amazon.smithy.rulesengine.language.eval.value.Value;
import software.amazon.smithy.rulesengine.language.syntax.Identifier;
import software.amazon.smithy.utils.MapUtils;

class RuleEngineTest {
    @Test
    void testRuleEval() throws IOException {
        EndpointRuleSet actual = TestDiscovery.getMinimalEndpointRuleSet();
        Value result = RuleEvaluator.evaluate(actual, MapUtils.of(Identifier.of("Region"),
                Value.stringValue("us-east-1")));
        EndpointValue expected = new EndpointValue.Builder(SourceLocation.none())
                .url("https://us-east-1.amazonaws.com")
                .putProperty("authSchemes", Value.arrayValue(Collections.singletonList(
                        Value.recordValue(MapUtils.of(
                                Identifier.of("name"), Value.stringValue("sigv4"),
                                Identifier.of("signingRegion"), Value.stringValue("us-east-1"),
                                Identifier.of("signingName"), Value.stringValue("serviceName")
                        ))
                )))
                .build();
        assertEquals(expected, result.expectEndpointValue());
    }
}
