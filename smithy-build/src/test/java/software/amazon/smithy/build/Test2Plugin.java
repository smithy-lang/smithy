/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build;

public final class Test2Plugin implements SmithyBuildPlugin {
    @Override
    public String getName() {
        return "test2";
    }

    @Override
    public void execute(PluginContext context) {
        context.getFileManifest().writeFile("hello2", "hi, test2!");
    }
}
