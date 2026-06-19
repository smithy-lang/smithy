/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.model.loader;

/**
 * Controls whether model loading parallelizes independent work (parsing files and building shapes).
 *
 * <p>Parallelism is enabled by default and can be disabled by setting the {@code smithy.parallelLoad} system
 * property to {@code "false"}, which is useful for debugging or for environments where the extra threads are
 * undesirable.
 */
final class ParallelLoading {

    private static final boolean ENABLED = !"false".equals(System.getProperty("smithy.parallelLoad"));

    private ParallelLoading() {}

    static boolean isEnabled() {
        return ENABLED;
    }
}
