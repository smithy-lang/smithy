/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.aws.rulesengine.language.functions.partition;

import java.io.InputStream;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public final class DefaultPartitionDataProvider implements PartitionDataProvider {
    @Override
    public Partitions loadPartitions() {
        InputStream json = DefaultPartitionDataProvider.class.getResourceAsStream("partitions.json");
        return Partitions.fromNode(Node.parse(json));
    }
}
