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
