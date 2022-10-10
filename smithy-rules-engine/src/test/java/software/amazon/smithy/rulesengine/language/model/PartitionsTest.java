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

package software.amazon.smithy.rulesengine.language.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;

public class PartitionsTest {

    @Test
    public void fromNode_propertiesDeserializedCorrectly() {
        Node json = Node.parse(PartitionsTest.class.getResourceAsStream("complete-partitions.json"));

        Partitions parsed = Partitions.fromNode(json);

        Partitions expected = Partitions.builder()
                .version("1.0")
                .addPartition(Partition.builder()
                        .id("aws")
                        .regionRegex("^(us|eu|ap|sa|ca|me|af)-\\w+-\\d+$")
                        .putRegion("ca-central-1", RegionOverride.builder().build())
                        .putRegion("us-west-2", RegionOverride.builder().build())
                        .outputs(PartitionOutputs.builder()
                                .dnsSuffix("amazonaws.com")
                                .dualStackDnsSuffix("api.aws")
                                .supportsFips(true)
                                .supportsDualStack(true)
                                .build())
                        .build())
                .addPartition(Partition.builder()
                        .id("aws-cn")
                        .regionRegex("^cn\\-\\w+\\-\\d+$")
                        .putRegion("cn-north-1", RegionOverride.builder().build())
                        .putRegion("cn-northwest-1", RegionOverride.builder().build())
                        .outputs(PartitionOutputs.builder()
                                .dnsSuffix("amazonaws.com.cn")
                                .dualStackDnsSuffix("api.amazonwebservices.com.cn")
                                .supportsFips(true)
                                .supportsDualStack(true)
                                .build())
                        .build())
                .build();

        assertThat(parsed, equalTo(expected));
    }
}
