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

package software.amazon.smithy.rulesengine.language.syntax.fn.partition;

import java.io.InputStream;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.rulesengine.language.model.Partitions;
import software.amazon.smithy.rulesengine.language.util.ResourceUtil;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class DefaultPartitionDataProvider implements PartitionDataProvider {
    private static final String DEFAULT_PARTITIONS_DATA =
            "/software/amazon/smithy/rulesengine/language/partitions.json";

    @Override
    public Partitions loadPartitions() {
        InputStream json = ResourceUtil.resourceAsStream(DEFAULT_PARTITIONS_DATA);
        return Partitions.fromNode(Node.parse(json));
    }
}
