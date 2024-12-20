/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build;

public class Test1ParallelPlugin implements SmithyBuildPlugin {
    @Override
    public String getName() {
        return "test1Parallel";
    }

    @Override
    public boolean isSerial() {
        return false;
    }

    @Override
    public void execute(PluginContext context) {
        context.getFileManifest().writeFile("hello1Parallel", String.format("%s", System.nanoTime()));
    }
}
