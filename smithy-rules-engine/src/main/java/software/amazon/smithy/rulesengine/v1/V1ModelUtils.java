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

package software.amazon.smithy.rulesengine.v1;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import software.amazon.smithy.model.node.ArrayNode;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.util.ResourceUtil;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class V1ModelUtils {
    private static final String V1_ENDPOINTS_JSON = "/software/amazon/smithy/rulesengine/language/endpoints.json";

    private static final String PARTITIONS = "partitions";

    private V1ModelUtils() {
    }

    public static List<PartitionV1> loadPartitionsFromClasspath() {
        return loadPartitionsFromStream(ResourceUtil.resourceAsStream(V1_ENDPOINTS_JSON));
    }

    public static List<PartitionV1> loadPartitionsFromStream(InputStream is) {
        ArrayNode partitionsNode = Node.parse(is).expectObjectNode().expectArrayMember(PARTITIONS);

        return partitionsNode.getElements()
                .stream()
                .map(Node::expectObjectNode)
                .map(PartitionV1::fromNode)
                .collect(Collectors.toList());
    }
}
