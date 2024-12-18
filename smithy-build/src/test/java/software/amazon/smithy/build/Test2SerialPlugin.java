/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package software.amazon.smithy.build;

public class Test2SerialPlugin implements SmithyBuildPlugin {
    @Override
    public String getName() {
        return "testSerial2";
    }

    @Override
    public boolean isSerial() {
        return true;
    }

    @Override
    public void execute(PluginContext context) {
        int accum = 0;
        for (int i = 0; i < 100000; i++) {
            accum++;
        }
        context.getFileManifest().writeFile("hello2Serial", String.format("%s", System.currentTimeMillis() + accum));
    }
}
