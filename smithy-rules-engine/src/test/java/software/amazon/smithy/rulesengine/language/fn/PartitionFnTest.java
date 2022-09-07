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
import software.amazon.smithy.rulesengine.language.eval.Scope;
import software.amazon.smithy.rulesengine.language.eval.Value;
import software.amazon.smithy.rulesengine.language.syntax.expr.Expr;
import software.amazon.smithy.rulesengine.language.syntax.fn.Fn;
import software.amazon.smithy.rulesengine.language.syntax.fn.PartitionFn;

public class PartitionFnTest {
    @Test
    public void eval() {
        Value.Record result = evalWithRegion("us-west-2");

        assertThat(result.get(PartitionFn.DNS_SUFFIX).expectString(), not(equalTo("")));
        assertThat(result.get(PartitionFn.DUAL_STACK_DNS_SUFFIX).expectString(), not(equalTo("")));
        assertThat(result.get(PartitionFn.SUPPORTS_FIPS).expectBool(), equalTo(true));
        assertThat(result.get(PartitionFn.SUPPORTS_DUAL_STACK).expectBool(), equalTo(true));
    }

    @Test
    public void eval_enumeratedRegion_inferredIsFalse() {
        Value.Record result = evalWithRegion("us-west-1");

        assertThat(result.get(PartitionFn.INFERRED).expectBool(), equalTo(false));
    }

    @Test
    public void eval_regionNotEnumerated_inferredIsTrue() {
        Value.Record result = evalWithRegion("us-west-3");

        assertThat(result.get(PartitionFn.INFERRED).expectBool(), equalTo(true));
    }


    private Value.Record evalWithRegion(String region) {
        Expr fn = Fn.fromNode(
                ObjectNode.builder().withMember("fn", PartitionFn.ID)
                        .withMember("argv", ArrayNode.arrayNode(StringNode.from(region)))
                        .build());

        return fn.eval(new Scope<>()).expectRecord();
    }
}
