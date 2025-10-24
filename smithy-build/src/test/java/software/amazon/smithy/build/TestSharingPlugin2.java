/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build;

public class TestSharingPlugin2 implements SmithyBuildPlugin {
    @Override
    public String getName() {
        return "testSharing2";
    }

    @Override
    public void execute(PluginContext context) {
        FileManifest manifest = context.getSharedFileManifest();
        String count = String.valueOf(manifest.getFiles().size() + 1);
        manifest.getFiles().forEach(file -> {
            manifest.writeFile(file, count);
        });
        manifest.writeFile("helloShare2", count);
    }
}
