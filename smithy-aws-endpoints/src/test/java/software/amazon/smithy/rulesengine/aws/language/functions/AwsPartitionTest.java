/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.rulesengine.aws.language.functions;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.aws.language.functions.partition.Partition;
import software.amazon.smithy.rulesengine.aws.language.functions.partition.PartitionOutputs;
import software.amazon.smithy.rulesengine.aws.language.functions.partition.Partitions;
import software.amazon.smithy.rulesengine.language.evaluation.RuleEvaluator;
import software.amazon.smithy.rulesengine.language.evaluation.value.RecordValue;
import software.amazon.smithy.rulesengine.language.syntax.expressions.Expression;

public class AwsPartitionTest {
    @Test
    public void eval() {
        RecordValue result = evalWithRegion("us-west-2");

        assertThat(result.get(AwsPartition.DNS_SUFFIX).expectStringValue().getValue(), not(equalTo("")));
        assertThat(result.get(AwsPartition.DUAL_STACK_DNS_SUFFIX).expectStringValue().getValue(), not(equalTo("")));
        assertThat(result.get(AwsPartition.SUPPORTS_FIPS).expectBooleanValue().getValue(), equalTo(true));
        assertThat(result.get(AwsPartition.SUPPORTS_DUAL_STACK).expectBooleanValue().getValue(), equalTo(true));
        assertThat(result.get(AwsPartition.IMPLICIT_GLOBAL_REGION).expectStringValue().getValue(), not(equalTo("")));
    }

    @Test
    public void enumeratedRegionIsNotInferred() {
        RecordValue result = evalWithRegion("us-west-1");

        assertThat(result.get(AwsPartition.INFERRED).expectBooleanValue().getValue(), equalTo(false));
    }

    @Test
    public void regionNotEnumeratedIsInferred() {
        RecordValue result = evalWithRegion("us-west-3");

        assertThat(result.get(AwsPartition.INFERRED).expectBooleanValue().getValue(), equalTo(true));
    }

    @Test
    public void overridesPartitions() {
        RecordValue result = evalWithRegion("us-west-1");
        assertThat(result.get(AwsPartition.INFERRED).expectBooleanValue().getValue(), equalTo(false));

        // Remove the enumerated regions, so the same region is now inferred.
        AwsPartition.overridePartitions(Partitions.builder()
                .addPartition(Partition.builder()
                        .id("aws")
                        .regionRegex("^(us|eu|ap|sa|ca|me|af)-\\w+-\\d+$")
                        .outputs(PartitionOutputs.builder()
                                .name("aws")
                                .dnsSuffix("amazonaws.com")
                                .dualStackDnsSuffix("api.aws")
                                .supportsFips(true)
                                .supportsDualStack(true)
                                .implicitGlobalRegion("us-east-1")
                                .build())
                        .build())
                .build());

        result = evalWithRegion("us-west-1");
        assertThat(result.get(AwsPartition.INFERRED).expectBooleanValue().getValue(), equalTo(true));

        // Set the partitions back to what they were.
        AwsPartition.overridePartitions(Partitions.fromNode(
                Node.parse(Partitions.class.getResourceAsStream("partitions.json"))));
    }

    private RecordValue evalWithRegion(String region) {
        AwsPartition fn = AwsPartition.ofExpressions(Expression.of(region));
        return fn.accept(new RuleEvaluator()).expectRecordValue();
    }
}
