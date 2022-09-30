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

package software.amazon.smithy.rulesengine.language.fn;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.ObjectNode;
import software.amazon.smithy.model.node.StringNode;
import software.amazon.smithy.rulesengine.language.eval.RuleEvaluator;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expression;
import software.amazon.smithy.rulesengine.language.syntax.fn.Function;
import software.amazon.smithy.rulesengine.language.stdlib.AwsPartition;

public class AwsPartitionFunctionTest {
    @Test
    public void eval() {
        Value.Record result = evalWithRegion("us-west-2");

        assertThat(result.get(AwsPartition.DNS_SUFFIX).expectString(), not(equalTo("")));
        assertThat(result.get(AwsPartition.DUAL_STACK_DNS_SUFFIX).expectString(), not(equalTo("")));
        assertThat(result.get(AwsPartition.SUPPORTS_FIPS).expectBool(), equalTo(true));
        assertThat(result.get(AwsPartition.SUPPORTS_DUAL_STACK).expectBool(), equalTo(true));
    }

    @Test
    public void eval_enumeratedRegion_inferredIsFalse() {
        Value.Record result = evalWithRegion("us-west-1");

        assertThat(result.get(AwsPartition.INFERRED).expectBool(), equalTo(false));
    }

    @Test
    public void eval_regionNotEnumerated_inferredIsTrue() {
        Value.Record result = evalWithRegion("us-west-3");

        assertThat(result.get(AwsPartition.INFERRED).expectBool(), equalTo(true));
    }


    private Value.Record evalWithRegion(String region) {
        Expression fn = Function.fromNode(
                ObjectNode.builder().withMember("fn", AwsPartition.ID)
                        .withMember("argv", ArrayNode.arrayNode(StringNode.from(region)))
                        .build());

        return fn.accept(new RuleEvaluator()).expectRecord();
    }
}
