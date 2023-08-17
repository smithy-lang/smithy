package software.amazon.smithy.rulesengine.aws.language.functions.partition;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.node.Node;

public class PartitionsTest {
    @Test
    public void roundtripsPartitions() {
        Node parsedNode = Node.parse(PartitionsTest.class.getResourceAsStream("complete-partitions.json"));
        Partitions parsed = Partitions.fromNode(parsedNode);

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
                                .implicitGlobalRegion("us-east-1")
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
                                .implicitGlobalRegion("cn-northwest-1")
                                .build())
                        .build())
                .build();
        Node expectedNode = expected.toNode();

        assertThat(parsed, equalTo(expected));
        Node.assertEquals(parsedNode, expectedNode);
    }
}
