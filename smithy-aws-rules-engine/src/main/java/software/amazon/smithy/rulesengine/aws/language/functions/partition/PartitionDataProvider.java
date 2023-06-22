/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package software.amazon.smithy.rulesengine.aws.language.functions.partition;

import software.amazon.smithy.utils.SmithyUnstableApi;

@SmithyUnstableApi
public interface PartitionDataProvider {
    Partitions loadPartitions();
}
